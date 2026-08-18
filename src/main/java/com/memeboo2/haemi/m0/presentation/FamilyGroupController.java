package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.command.CreateFamilyGroupCommand;
import com.memeboo2.haemi.m0.application.command.CreateInvitationCommand;
import com.memeboo2.haemi.m0.application.dto.FamilyGroupResult;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;
import com.memeboo2.haemi.m0.application.dto.InvitationResult;
import com.memeboo2.haemi.m0.application.dto.OwnershipTransferResult;
import com.memeboo2.haemi.m0.application.service.FamilyGroupApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.*;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "M0-FamilyGroup", description = "F0-01 계정과 가족 그룹 관리")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class FamilyGroupController {

    private final FamilyGroupApplicationService familyGroups;

    @Operation(summary = "가족 그룹 생성", description = "요청한 인증 사용자를 대표 보호자(owner)로 추가합니다.")
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FamilyGroupResult> create(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.ok(familyGroups.create(member.memberId(),
                new CreateFamilyGroupCommand(request.relation(), request.notificationPreference())));
    }

    @Operation(summary = "가족 구성원 및 어르신 초대",
            description = """
                    대표 보호자만 72시간 유효한 초대를 발급할 수 있습니다.
                    kind=FAMILY(기본)는 이메일·관계가 필수이며 링크 토큰을 발급합니다.
                    kind=ELDER는 어르신 화면에서 입력할 6자리 코드를 발급하며, 어르신은 구성원 정원(10명)에 포함되지 않습니다.
                    """)
    @PostMapping("/groups/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResult> invite(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return ApiResponse.ok(familyGroups.invite(member.memberId(), groupId,
                new CreateInvitationCommand(
                        request.kind() == null ? InvitationKind.FAMILY : request.kind(),
                        request.email(), request.relation())));
    }

    @Operation(summary = "초대 수락", description = "초대 링크를 가진 로그인 사용자를 가족 그룹 구성원으로 등록합니다.")
    @PostMapping("/invitations/{token}/accept")
    public ApiResponse<FamilyGroupResult> acceptInvitation(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String token) {
        return ApiResponse.ok(familyGroups.acceptInvitation(member.memberId(), token));
    }

    @Operation(summary = "가족 구성원 역할 변경", description = "대표 보호자 권한은 이양 절차로만 변경됩니다.")
    @PatchMapping("/groups/{groupId}/members/{memberId}")
    public ApiResponse<FamilyGroupResult> changeMemberRole(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID groupId, @PathVariable UUID memberId,
            @Valid @RequestBody ChangeMemberRoleRequest request) {
        return ApiResponse.ok(familyGroups.changeMemberRole(member.memberId(), groupId, memberId, request.role()));
    }

    @Operation(summary = "가족 구성원 제외", description = "작성한 추억은 유지되며 이후 표기는 가족으로 익명화됩니다.")
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AuthenticatedMember member,
                             @PathVariable UUID groupId, @PathVariable UUID memberId) {
        familyGroups.removeMember(member.memberId(), groupId, memberId);
    }

    @Operation(summary = "대표 보호자 이양 요청", description = "대상 구성원의 수락 후에만 권한이 이양됩니다.")
    @PostMapping("/groups/{groupId}/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnershipTransferResult> requestTransfer(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID groupId,
            @Valid @RequestBody OwnershipTransferRequest request) {
        return ApiResponse.ok(familyGroups.requestOwnershipTransfer(member.memberId(), groupId,
                request.recipientMemberId()));
    }

    @Operation(summary = "대표 보호자 이양 수락")
    @PostMapping("/groups/{groupId}/transfer/{transferId}/accept")
    public ApiResponse<FamilyGroupResult> acceptTransfer(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID groupId, @PathVariable UUID transferId) {
        return ApiResponse.ok(familyGroups.acceptOwnershipTransfer(member.memberId(), groupId, transferId));
    }

    @Operation(summary = "가족 그룹 조회")
    @GetMapping("/groups/{groupId}")
    public ApiResponse<FamilyGroupResult> get(@AuthenticationPrincipal AuthenticatedMember member,
                                               @PathVariable UUID groupId) {
        return ApiResponse.ok(familyGroups.get(member.memberId(), groupId));
    }
}
