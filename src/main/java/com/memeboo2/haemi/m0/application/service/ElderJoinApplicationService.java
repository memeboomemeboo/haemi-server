package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.PasswordEncoderPort;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.application.command.AcceptElderInvitationCommand;
import com.memeboo2.haemi.m0.application.command.RefreshElderSessionCommand;
import com.memeboo2.haemi.m0.application.dto.ElderSessionResult;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderSession;
import com.memeboo2.haemi.m0.domain.model.ElderSessionRevokeReason;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.model.M0ConflictException;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.ElderSessionRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m0.domain.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * F0-01-E 어르신 참여 로그인.
 *
 * <p>어르신은 성함·전화번호·초대 코드를 최초 1회 입력해 그룹의 elder로 합류하고, 이후에는
 * 평생 세션(rolling refresh)으로 자동 로그인한다. 초대 코드가 어느 그룹인지 결정하는 신뢰
 * 요소이고, 성함은 그 코드가 이 어르신 본인에게 쓰였는지 교차 검증하며, 전화번호는 OTP 없이
 * 식별·중복 감지에만 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ElderJoinApplicationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 코드·성함·번호 어느 쪽이 틀렸는지 알려주지 않는다. 대입 공격에 힌트를 주지 않기 위해서다. */
    private static final String INVALID_CODE_MESSAGE = "코드를 다시 확인해주세요.";

    private final InvitationRepository invitations;
    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final ElderSessionRepository sessions;
    private final MemberRepository members;
    private final PhoneHasher phoneHasher;
    private final TokenPort tokenPort;
    private final PasswordEncoderPort passwordEncoder;

    @Value("${haemi.elder.session.rolling-days:180}")
    private long rollingDays;

    /** 어르신 화면의 [들어가기] 1회. 성공하면 홈으로 직행할 수 있는 토큰 쌍을 돌려준다. */
    public ElderSessionResult join(AcceptElderInvitationCommand command) {
        Invitation invitation = invitations.findPendingElderInvitationByCode(normalizeCode(command.code()))
                .orElseThrow(() -> new M0ValidationException(INVALID_CODE_MESSAGE));
        invitation.recordJoinAttempt();
        invitations.save(invitation);

        Elder elder = elders.findByGroupId(invitation.getGroupId())
                .orElseThrow(() -> new M0NotFoundException("어르신 프로필"));

        // 사별·추모 상태에서는 어르신 화면 자체를 열지 않는다 (EX-F005-01과 같은 관문).
        // 입원·휴면은 발송만 멈추는 상태이므로 합류·세션은 막지 않는다.
        if (!elder.getStatus().isLiving()) {
            throw new M0ConflictException("지금은 들어갈 수 없어요. 가족에게 확인을 부탁드려요.");
        }

        if (!elder.matchesName(command.name())) {
            invitation.hold(LocalDateTime.now());
            invitations.save(invitation);
            throw new M0ConflictException("정보가 맞는지 확인 중이에요.");
        }

        String phoneHash = phoneHasher.hash(command.phoneNumber());
        elders.findByPhoneHash(phoneHash)
                .filter(other -> !other.getId().equals(elder.getId()))
                .ifPresent(other -> {
                    throw new M0ConflictException("이미 참여 중인 가족이 있어요.");
                });
        elder.registerPhoneHash(phoneHash);

        Member elderAccount = resolveElderAccount(elder);
        elder.linkMember(elderAccount.getId());
        elders.save(elder);

        // 같은 기기에서 다시 합류하면 이전 세션을 끊고 새로 발급한다(기기당 활성 세션 1개).
        sessions.findActiveByElderIdAndDeviceId(elder.getId(), command.deviceId().trim())
                .ifPresent(existing -> {
                    existing.revoke(ElderSessionRevokeReason.REISSUED, LocalDateTime.now());
                    sessions.save(existing);
                });

        String refreshToken = newRefreshToken();
        ElderSession session = sessions.save(ElderSession.issue(elder.getId(), elder.getGroupId(),
                command.deviceId(), tokenPort.hashToken(refreshToken), LocalDateTime.now(), rollingDays));

        invitation.accept();
        invitations.save(invitation);

        return ElderSessionResult.of(session, elder.getName(),
                accessTokenFor(elderAccount), refreshToken);
    }

    /** 앱 실행 시 silent refresh. 사용할 때마다 만료를 연장해 재로그인을 요구하지 않는다. */
    public ElderSessionResult refresh(RefreshElderSessionCommand command) {
        ElderSession session = sessions.findByRefreshTokenHash(tokenPort.hashToken(command.refreshToken()))
                .orElseThrow(() -> new M0ValidationException("다시 들어와 주세요."));
        if (!session.isUsableAt(LocalDateTime.now()) || !session.matchesDevice(command.deviceId())) {
            throw new M0ValidationException("다시 들어와 주세요.");
        }
        Elder elder = elders.findById(session.getElderId())
                .orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
        if (!elder.getStatus().isLiving()) {
            session.revoke(ElderSessionRevokeReason.ELDER_STATUS_CHANGED, LocalDateTime.now());
            sessions.save(session);
            throw new M0ValidationException("다시 들어와 주세요.");
        }
        Member elderAccount = members.findById(elder.getMemberId())
                .orElseThrow(() -> new M0NotFoundException("어르신 계정"));

        String refreshToken = newRefreshToken();
        session.rotate(tokenPort.hashToken(refreshToken), LocalDateTime.now(), rollingDays);
        sessions.save(session);

        return ElderSessionResult.of(session, elder.getName(), accessTokenFor(elderAccount), refreshToken);
    }

    /** owner만 어르신 기기 연결을 초기화할 수 있다. 어르신 화면에는 로그아웃 UI를 두지 않는다. */
    public int revokeByOwner(UUID actorId, UUID elderId) {
        Elder elder = elders.findById(elderId).orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
        FamilyGroup group = groups.findById(elder.getGroupId())
                .orElseThrow(() -> new M0NotFoundException("가족 그룹"));
        group.requireOwner(actorId);
        return revokeAll(elderId, ElderSessionRevokeReason.OWNER_REVOKED);
    }

    /** 상태 변경(F0-05)으로 어르신 화면을 즉시 닫는다. 기기 원격 잠금(EX-F005-06)의 서버 측 관문이다. */
    public int revokeAll(UUID elderId, ElderSessionRevokeReason reason) {
        LocalDateTime now = LocalDateTime.now();
        var active = sessions.findActiveByElderId(elderId);
        active.forEach(session -> {
            session.revoke(reason, now);
            sessions.save(session);
        });
        return active.size();
    }

    /**
     * 사별 오등록 복구(48시간 내)에서만 세션을 되살린다. 복구인데도 어르신이 재로그인해야 하면
     * 조작 부담이 그대로 어르신에게 돌아간다(F0-01-E 검증 지표: 재로그인 요구 0건).
     */
    public int restoreAfterBereavementRecovery(UUID elderId, LocalDateTime revokedSince) {
        LocalDateTime now = LocalDateTime.now();
        var revoked = sessions.findRevokedSince(elderId, revokedSince).stream()
                .filter(session -> session.getRevokedReason() == ElderSessionRevokeReason.ELDER_STATUS_CHANGED)
                .toList();
        revoked.forEach(session -> {
            session.restore(now, rollingDays);
            sessions.save(session);
        });
        return revoked.size();
    }

    /**
     * 어르신 기기의 인증 주체는 기존 ELDER 계정 모델을 그대로 쓴다. 이미 어르신 전용 API가
     * {@code hasRole('ELDER')}와 {@code Elder.memberId} 연결로 대상을 판정하고 있어, 별도
     * principal을 새로 만들면 그 검증들을 모두 이중화해야 한다. 비밀번호 로그인은 쓰지 않으므로
     * 자격증명은 임의값으로 두고 세션은 elder_session이 관리한다.
     */
    private Member resolveElderAccount(Elder elder) {
        if (elder.getMemberId() != null) {
            return members.findById(elder.getMemberId())
                    .orElseThrow(() -> new M0NotFoundException("어르신 계정"));
        }
        Member account = Member.create(syntheticEmail(elder.getId()),
                passwordEncoder.encode(newRefreshToken()), elder.getName(), MemberRole.ELDER);
        account.verifyEmail();
        return members.save(account);
    }

    private String accessTokenFor(Member account) {
        return tokenPort.generateAccessToken(account.getId(), account.getEmail(), account.getRole());
    }

    private String syntheticEmail(UUID elderId) {
        return "elder-" + elderId + "@elder.haemi.invalid";
    }

    private String normalizeCode(String code) {
        String digits = code == null ? "" : code.replaceAll("[^0-9]", "");
        if (digits.length() != 6) {
            throw new M0ValidationException(INVALID_CODE_MESSAGE);
        }
        return digits;
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
