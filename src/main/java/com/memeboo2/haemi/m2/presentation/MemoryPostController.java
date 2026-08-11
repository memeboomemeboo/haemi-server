package com.memeboo2.haemi.m2.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.application.command.*;
import com.memeboo2.haemi.m2.application.dto.MemoryPostResult;
import com.memeboo2.haemi.m2.application.query.GeneratePoemDraftQuery;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import com.memeboo2.haemi.m2.presentation.dto.request.CreateMemoryPostRequest;
import com.memeboo2.haemi.m2.presentation.dto.request.ReplyToPostRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "M2-Post", description = "F2-01 추억글 작성 / F2-02 어르신 알림 / F2-03 어르신 답변")
@RestController
@RequestMapping("/api/v1/albums/{albumId}/posts")
@RequiredArgsConstructor
public class MemoryPostController {

    private final MemoryPostApplicationService postService;

    @Operation(
            summary = "추억글 작성 [F2-01]",
            description = """
                    가족이 텍스트·사진·음성으로 추억글을 작성합니다.

                    **콘텐츠 규칙:**
                    - 텍스트·사진·음성 중 **최소 1가지 이상** 필수
                    - 본문: 최대 500자
                    - 사진: 최대 3장, 장당 10MB
                    - 음성: MP3/M4A/AAC, 최대 3분

                    `publishImmediately=false`이면 임시 저장됩니다 (최대 3개 보관).
                    게시 시 금칙어 필터링이 자동으로 실행됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "콘텐츠 없음 / 본문 500자 초과 / 사진 3장 초과 / 파일 크기 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
                    description = "금칙어 감지")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemoryPostResult> createPost(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @RequestPart("data") @Valid CreateMemoryPostRequest request,
            @Parameter(description = "첨부 사진 (최대 3장)")
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @Parameter(description = "음성 메모 (MP3/M4A/AAC, 최대 3분)")
            @RequestPart(value = "voice", required = false) MultipartFile voice) throws IOException {

        List<CreateMemoryPostCommand.PhotoAttachment> photoAttachments = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile p : photos) {
                photoAttachments.add(new CreateMemoryPostCommand.PhotoAttachment(
                        p.getInputStream(), p.getOriginalFilename(),
                        p.getContentType(), p.getSize()));
            }
        }

        CreateMemoryPostCommand.VoiceMemo voiceMemo = null;
        if (voice != null) {
            voiceMemo = new CreateMemoryPostCommand.VoiceMemo(
                    voice.getInputStream(), voice.getContentType(), 0L);
        }

        MemoryPostResult result = postService.createPost(new CreateMemoryPostCommand(
                albumId, request.memberId(), request.memberName(), request.relation(),
                request.textContent(), photoAttachments, voiceMemo,
                request.publishImmediately()
        ));
        return ApiResponse.ok(result, request.publishImmediately()
                ? "추억글이 게시되었습니다." : "임시 저장되었습니다.");
    }

    @Operation(
            summary = "임시 저장 게시 [F2-01]",
            description = "임시 저장된 추억글을 게시합니다. 게시 전 금칙어 재검사를 수행합니다."
    )
    @PostMapping("/{postId}/publish")
    public ApiResponse<MemoryPostResult> publishDraft(
            @PathVariable String albumId,
            @PathVariable String postId,
            @RequestParam String memberId) {
        return ApiResponse.ok(postService.publishDraft(
                new PublishDraftCommand(postId, memberId)), "게시되었습니다.");
    }

    @Operation(
            summary = "어르신 열람 처리 [F2-02]",
            description = "어르신이 추억글을 열람했을 때 호출합니다. 가족 앱에 '읽음' 표시가 업데이트됩니다."
    )
    @PatchMapping("/{postId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable String albumId,
            @PathVariable String postId) {
        postService.markReadByElder(postId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "어르신 추억글 답변 [F2-02]",
            description = """
                    어르신이 받은 추억글에 음성 또는 마음 이모지로 답변합니다. 텍스트 직접 입력은 제공하지 않으며 즉시 전송됩니다.

                    **답변 유형:**
                    - `VOICE`: 음성 파일 첨부 → STT 변환 후 텍스트로 저장 (권장·1순위)
                    - `EMOJI`: 마음 이모지 6종(❤️ 🤗 😊 🥹 🙏 🥰) 중 하나
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "음성 형식·크기 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "음성 전사 요청 과다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI가 요청을 거절함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 음성 전사 일시 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 답변한 추억글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "추억글 없음")
    })
    @PostMapping(value = "/{postId}/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemoryPostResult> replyToPost(
            @PathVariable String albumId,
            @PathVariable String postId,
            @RequestPart("data") @Valid ReplyToPostRequest request,
            @Parameter(description = "음성 입력 파일 (VOICE 유형일 때 STT 변환). application/octet-stream은 mp3·m4a 등 파일 확장자로 형식을 판별합니다.")
            @RequestPart(value = "voice", required = false) MultipartFile voice) throws IOException {

        MemoryPostResult result = postService.replyToPost(new ReplyToPostCommand(
                postId, request.elderId(), request.replyType(),
                request.heartEmojiCode(),
                voice != null ? voice.getInputStream() : null,
                voice != null ? voice.getContentType() : null,
                voice != null ? voice.getOriginalFilename() : null
        ));
        return ApiResponse.ok(result, "답장을 보냈습니다.");
    }

    @Operation(
            summary = "AI 시 초안 생성 [F2-03]",
            description = "추억글 내용을 기반으로 Gemini가 시 초안을 생성합니다. AI 설정 또는 업스트림이 일시 불가하면 503을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시 초안 생성 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "AI 요청 과다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI가 요청을 거절함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 시 생성 일시 불가")
    })
    @GetMapping("/{postId}/poem-draft")
    public ApiResponse<String> getPoemDraft(
            @PathVariable String albumId,
            @PathVariable String postId) {
        return ApiResponse.ok(postService.generatePoemDraft(new GeneratePoemDraftQuery(postId)));
    }

    @Operation(summary = "좋아요 토글 [F2-04]", description = "이미 좋아요한 경우 취소됩니다.")
    @PostMapping("/{postId}/like")
    public ApiResponse<MemoryPostResult> toggleLike(
            @PathVariable String albumId,
            @PathVariable String postId,
            @RequestParam String memberId) {
        return ApiResponse.ok(postService.toggleLike(new LikePostCommand(postId, memberId)));
    }

    @Operation(summary = "추억글 삭제", description = "본인이 작성한 추억글만 삭제할 수 있습니다.")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable String albumId,
            @PathVariable String postId,
            @RequestParam String memberId) {
        postService.deletePost(new DeletePostCommand(postId, memberId));
    }
}
