package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.port.ElderAccessPort;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m1.application.command.CreateMemoryCommand;
import com.memeboo2.haemi.m1.application.dto.MemoryFeedResult;
import com.memeboo2.haemi.m1.application.dto.MemoryResult;
import com.memeboo2.haemi.m1.domain.model.memory.*;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryApplicationService {

    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_AUDIO_DURATION_MS = 3L * 60 * 1000;

    private final FamilyGroupRepository groups;
    private final MemberRepository members;
    private final MemoryRepository memories;
    private final PhotoStoragePort storage;
    private final MemoryModerationService moderation;
    private final ElderAccessPort elderAccess;

    @Transactional
    public MemoryResult create(CreateMemoryCommand command) {
        FamilyGroup group = loadGroup(command.groupId());
        group.requireActiveMember(command.authorUserId());

        MemoryModerationStatus moderationStatus = moderation.inspect(command.textContent());
        if (moderationStatus == MemoryModerationStatus.BLOCKED) {
            throw new MemoryModerationException();
        }

        String authorName = members.findById(command.authorUserId())
                .map(member -> member.getName())
                .orElse("가족");
        String relation = group.getActiveMembers().stream()
                .filter(member -> member.getMemberId().equals(command.authorUserId()))
                .findFirst()
                .map(member -> member.getRelation().name())
                .orElse("FAMILY");
        Memory memory = Memory.create(command.groupId(), command.authorUserId(), command.textContent(),
                authorName, relation, command.visibility(), moderationStatus);

        List<String> storedKeys = new ArrayList<>();
        try {
            List<CreateMemoryCommand.MediaAttachment> attachments = command.media() == null ? List.of() : command.media();
            for (int index = 0; index < attachments.size(); index++) {
                CreateMemoryCommand.MediaAttachment attachment = attachments.get(index);
                validateAttachment(attachment);
                String storageKey = storage.store(attachment.inputStream(), attachment.originalFilename(),
                        attachment.contentType());
                storedKeys.add(storageKey);
                memory.addMedia(attachment.type(), storageKey, null, attachment.durationMs(), index);
            }
            memory.validatePublishable();
            Memory saved = memories.save(memory);
            log.info("통합 추억 생성: memoryId={}, groupId={}, visibility={}, moderation={}",
                    saved.getId(), saved.getGroupId(), saved.getVisibility(), saved.getModerationStatus());
            return MemoryResult.from(saved, storage);
        } catch (RuntimeException e) {
            cleanup(storedKeys);
            throw e;
        } catch (Exception e) {
            cleanup(storedKeys);
            throw new MemoryValidationException("추억 파일을 저장하지 못했어요.");
        }
    }

    @Transactional(readOnly = true)
    public MemoryFeedResult getFamilyFeed(UUID actorId, UUID groupId, int page, int size) {
        FamilyGroup group = loadGroup(groupId);
        group.requireActiveMember(actorId);
        Page<Memory> feed = memories.findFamilyFeed(groupId, pageable(page, size));
        return feedResult(feed, true);
    }

    /**
     * 어르신 기기용 조회. 쿼리와 애플리케이션 모두에서 FAMILY_ONLY를 배제한다.
     * 상태가 사망·입원·휴면이면 게시물은 보존하되 전달 가능한 피드를 반환하지 않는다.
     */
    @Transactional(readOnly = true)
    public MemoryFeedResult getElderFeed(UUID elderId, int page, int size) {
        ElderAccessPort.ElderAccessSnapshot elder = elderAccess.getRequired(elderId);
        if (!elder.isElderFacingDeliveryAllowed()) {
            return new MemoryFeedResult(List.of(), 0, page, size, false, false);
        }
        Page<Memory> feed = memories.findElderFeed(elder.groupId(), pageable(page, size));
        // Repository 조건이 변경돼도 S1 경계가 무너지지 않도록 이중 검증한다.
        List<MemoryResult> visible = feed.getContent().stream()
                .filter(Memory::isElderVisible)
                .map(memory -> MemoryResult.from(memory, storage))
                .toList();
        return new MemoryFeedResult(visible, visible.size(), page, size,
                feed.hasNext() && visible.size() == feed.getNumberOfElements(), true);
    }

    @Transactional(readOnly = true)
    public MemoryFeedResult getPendingModeration(UUID actorId, UUID groupId, int page, int size) {
        FamilyGroup group = loadGroup(groupId);
        group.requireOwner(actorId);
        return feedResult(memories.findByGroupIdAndModerationStatus(groupId, MemoryModerationStatus.REVIEW,
                pageable(page, size)), true);
    }

    @Transactional
    public MemoryResult approveModeration(UUID actorId, UUID groupId, UUID memoryId) {
        FamilyGroup group = loadGroup(groupId);
        group.requireOwner(actorId);
        Memory memory = loadMemory(memoryId);
        requireSameGroup(groupId, memory);
        memory.approveModeration();
        return MemoryResult.from(memories.save(memory), storage);
    }

    @Transactional
    public void delete(UUID actorId, UUID groupId, UUID memoryId) {
        FamilyGroup group = loadGroup(groupId);
        group.requireActiveMember(actorId);
        Memory memory = loadMemory(memoryId);
        requireSameGroup(groupId, memory);
        if (!memory.canDelete(actorId, group.getOwnerMemberId())) {
            throw new MemoryAccessDeniedException();
        }
        List<String> storageKeys = memory.getMedia().stream().map(MemoryMedia::getStorageKey).toList();
        memories.delete(memory);
        cleanup(storageKeys);
    }

    private MemoryFeedResult feedResult(Page<Memory> feed, boolean deliveryAvailable) {
        return new MemoryFeedResult(feed.getContent().stream().map(memory -> MemoryResult.from(memory, storage)).toList(),
                feed.getTotalElements(), feed.getNumber(), feed.getSize(), feed.hasNext(), deliveryAvailable);
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new MemoryValidationException("페이지는 0 이상, 크기는 1~50 사이여야 해요.");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void validateAttachment(CreateMemoryCommand.MediaAttachment attachment) {
        if (attachment == null || attachment.type() == null || attachment.inputStream() == null
                || attachment.sizeBytes() < 1 || attachment.originalFilename() == null) {
            throw new MemoryValidationException("첨부 파일 정보가 올바르지 않아요.");
        }
        String contentType = attachment.contentType() == null ? "" : attachment.contentType().toLowerCase();
        if (attachment.type() == MemoryMediaType.IMAGE) {
            if (attachment.sizeBytes() > MAX_IMAGE_SIZE_BYTES || !isSupportedImage(contentType)) {
                throw new MemoryValidationException("사진은 JPG, PNG, HEIC, WebP 형식의 20MB 이하 파일만 가능해요.");
            }
        } else if (attachment.type() == MemoryMediaType.AUDIO) {
            if (!isSupportedAudio(contentType) || attachment.durationMs() == null
                    || attachment.durationMs() < 1 || attachment.durationMs() > MAX_AUDIO_DURATION_MS) {
                throw new MemoryValidationException("음성은 M4A/AAC 형식의 3분 이하 파일만 가능해요.");
            }
        }
    }

    private boolean isSupportedImage(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/png")
                || contentType.equals("image/heic") || contentType.equals("image/webp");
    }

    private boolean isSupportedAudio(String contentType) {
        return contentType.equals("audio/mp4") || contentType.equals("audio/x-m4a")
                || contentType.equals("audio/aac");
    }

    private FamilyGroup loadGroup(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> new M0NotFoundException("가족 그룹"));
    }

    private Memory loadMemory(UUID memoryId) {
        return memories.findById(memoryId).orElseThrow(() -> new MemoryNotFoundException(memoryId));
    }

    private void requireSameGroup(UUID groupId, Memory memory) {
        if (!groupId.equals(memory.getGroupId())) {
            throw new MemoryNotFoundException(memory.getId());
        }
    }

    private void cleanup(List<String> storageKeys) {
        storageKeys.forEach(storage::delete);
    }
}
