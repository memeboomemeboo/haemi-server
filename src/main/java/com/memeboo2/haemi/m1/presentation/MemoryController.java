package com.memeboo2.haemi.m1.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m1.application.command.CreateMemoryCommand;
import com.memeboo2.haemi.m1.application.dto.MemoryFeedResult;
import com.memeboo2.haemi.m1.application.dto.MemoryResult;
import com.memeboo2.haemi.m1.application.service.MemoryApplicationService;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryMediaType;
import com.memeboo2.haemi.m1.presentation.dto.request.CreateMemoryRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Tag(name = "M1-Memory", description = "F1-03 사진·글·음성 통합 추억 피드")
@RestController
@RequestMapping("/api/v1/groups/{groupId}/memories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class MemoryController {

    private final MemoryApplicationService memories;

    @Operation(summary = "추억 남기기", description = "사진(최대 10장)·글·음성(M4A/AAC, 3분) 중 하나 이상을 하나의 추억으로 남깁니다. family_only는 어르신에게 전달되지 않습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemoryResult> create(@AuthenticationPrincipal AuthenticatedMember member,
                                             @PathVariable UUID groupId,
                                             @Valid @RequestPart("data") CreateMemoryRequest request,
                                             @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                             @RequestPart(value = "audio", required = false) MultipartFile audio,
                                             @RequestParam(required = false) Long audioDurationMs) throws IOException {
        List<CreateMemoryCommand.MediaAttachment> attachments = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                attachments.add(new CreateMemoryCommand.MediaAttachment(MemoryMediaType.IMAGE,
                        image.getInputStream(), image.getOriginalFilename(), image.getContentType(),
                        image.getSize(), null));
            }
        }
        if (audio != null && !audio.isEmpty()) {
            attachments.add(new CreateMemoryCommand.MediaAttachment(MemoryMediaType.AUDIO,
                    audio.getInputStream(), audio.getOriginalFilename(), audio.getContentType(),
                    audio.getSize(), audioDurationMs));
        }
        MemoryResult result = memories.create(new CreateMemoryCommand(groupId, member.memberId(),
                request.textContent(), request.visibility(), attachments));
        String message = result.moderationStatus().name().equals("REVIEW")
                ? "대표 보호자의 확인 후 게시됩니다." : "추억이 피드에 추가되었습니다.";
        return ApiResponse.ok(result, message);
    }

    @Operation(summary = "가족 추억 피드 조회", description = "가족 구성원은 group_all과 family_only의 안전 검토 완료 추억을 시간순으로 조회합니다.")
    @GetMapping
    public ApiResponse<MemoryFeedResult> getFamilyFeed(@AuthenticationPrincipal AuthenticatedMember member,
                                                        @PathVariable UUID groupId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(memories.getFamilyFeed(member.memberId(), groupId, page, size));
    }

    @Operation(summary = "검토 대기 추억 조회", description = "대표 보호자만 민감 표현으로 검토 대기 중인 추억을 확인합니다.")
    @GetMapping("/moderation/pending")
    public ApiResponse<MemoryFeedResult> getPendingModeration(@AuthenticationPrincipal AuthenticatedMember member,
                                                               @PathVariable UUID groupId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(memories.getPendingModeration(member.memberId(), groupId, page, size));
    }

    @Operation(summary = "검토 대기 추억 승인", description = "대표 보호자 확인 후에만 피드와 어르신 전달 대상에 반영합니다.")
    @PostMapping("/{memoryId}/moderation/approve")
    public ApiResponse<MemoryResult> approveModeration(@AuthenticationPrincipal AuthenticatedMember member,
                                                        @PathVariable UUID groupId, @PathVariable UUID memoryId) {
        return ApiResponse.ok(memories.approveModeration(member.memberId(), groupId, memoryId));
    }

    @Operation(summary = "추억 삭제", description = "대표 보호자는 전체, 일반 가족은 본인 작성 추억만 삭제할 수 있습니다.")
    @DeleteMapping("/{memoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedMember member,
                       @PathVariable UUID groupId, @PathVariable UUID memoryId) {
        memories.delete(member.memberId(), groupId, memoryId);
    }
}
