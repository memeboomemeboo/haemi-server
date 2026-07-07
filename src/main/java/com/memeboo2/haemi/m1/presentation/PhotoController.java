package com.memeboo2.haemi.m1.presentation;

import com.memeboo2.haemi.m1.application.command.RemovePhotoCommand;
import com.memeboo2.haemi.m1.application.command.SavePhotoCommand;
import com.memeboo2.haemi.m1.application.command.SyncPhotosCommand;
import com.memeboo2.haemi.m1.application.command.UpdatePhotoMemoCommand;
import com.memeboo2.haemi.m1.application.dto.PhotoResult;
import com.memeboo2.haemi.m1.application.dto.SyncHistoryResult;
import com.memeboo2.haemi.m1.application.query.GetSyncHistoryQuery;
import com.memeboo2.haemi.m1.application.service.PhotoApplicationService;
import com.memeboo2.haemi.m1.domain.model.album.NetworkType;
import com.memeboo2.haemi.m1.presentation.dto.request.UpdatePhotoMemoRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Tag(name = "M1-Photo", description = "F1-01 사진 개별 저장 / F1-02 일괄 동기화 / F1-04 메모 & 회상 태깅")
@RestController
@RequestMapping("/api/v1/albums/{albumId}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoApplicationService photoService;

    @Operation(
            summary = "사진 개별 저장 [F1-01]",
            description = """
                    갤러리에서 선택한 사진을 기억 앨범에 저장합니다.

                    **지원 형식:** JPG, PNG, HEIC, WebP
                    **최대 크기:** 파일당 20MB
                    **최대 장수:** 1회 30장

                    - HEIC는 서버에서 JPG로 자동 변환됩니다.
                    - SHA-256 해시로 중복 사진을 자동 차단합니다.
                    - 저장 후 AI 분석 큐에 자동 등록되고, 완료 시 푸시 알림이 발송됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "파일 형식 오류 / 용량 초과 / 30장 초과",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 저장된 사진(중복)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<PhotoResult>> savePhotos(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "업로더 memberId", required = true) @RequestParam String uploadedBy,
            @Parameter(description = "사진 파일 (최대 30장)", required = true)
            @RequestPart("files") List<MultipartFile> files) throws IOException {

        if (files.size() > 30) {
            return ApiResponse.error("1회 최대 30장까지 업로드 가능합니다.");
        }

        List<PhotoResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            SavePhotoCommand cmd = new SavePhotoCommand(
                    albumId, uploadedBy,
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    computeHash(file),
                    null, null, null
            );
            results.add(photoService.savePhoto(cmd));
        }
        return ApiResponse.ok(results, results.size() + "장 저장되었습니다.");
    }

    @Operation(
            summary = "전체 사진 일괄 동기화 [F1-02]",
            description = """
                    기기 내 사진을 일괄 동기화합니다. **증분 방식**으로 이미 동기화된 사진은 건너뜁니다.

                    - SHA-256 해시 비교로 중복을 자동 제거합니다.
                    - 응답의 `skipped` 필드에서 건너뛴 파일명을 확인할 수 있습니다.
                    - Wi-Fi 전용 설정(`wifiOnly`, 기본값 true) 상태에서 셀룰러 연결이면 동기화가 거부됩니다.
                    - 배터리 20% 이하(`batteryLevel`)에서는 동기화가 거부되고, 충전 후 재개해야 합니다.
                    - 동기화 결과는 날짜별 이력으로 기록되며 `/sync/history`에서 조회할 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동기화 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Wi-Fi 전용 설정 중 셀룰러 연결 / 배터리 20% 이하"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @PostMapping(value = "/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PhotoApplicationService.SyncResult> syncPhotos(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "업로더 memberId", required = true) @RequestParam String uploadedBy,
            @Parameter(description = "사진 파일 목록", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "Wi-Fi 전용 동기화 여부") @RequestParam(defaultValue = "true") boolean wifiOnly,
            @Parameter(description = "현재 기기 네트워크 종류") @RequestParam(defaultValue = "UNKNOWN") NetworkType networkType,
            @Parameter(description = "현재 기기 배터리 잔량(%)") @RequestParam(required = false) Integer batteryLevel,
            @Parameter(description = "백그라운드 동기화 여부") @RequestParam(defaultValue = "false") boolean backgroundSync
    ) throws IOException {

        List<SavePhotoCommand> cmds = new ArrayList<>();
        for (MultipartFile file : files) {
            cmds.add(new SavePhotoCommand(
                    albumId, uploadedBy,
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    computeHash(file),
                    null, null, null
            ));
        }

        PhotoApplicationService.SyncResult result = photoService.syncPhotos(
                new SyncPhotosCommand(albumId, uploadedBy, cmds, wifiOnly, networkType, batteryLevel, backgroundSync));
        return ApiResponse.ok(result, "총 " + result.saved().size() + "장 동기화 완료");
    }

    @Operation(
            summary = "동기화 이력 조회 [F1-02]",
            description = "날짜별로 동기화된 사진 수(요청/저장/건너뜀)와 동기화 시점의 네트워크·배터리 상태를 최신순으로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @GetMapping("/sync/history")
    public ApiResponse<List<SyncHistoryResult>> getSyncHistory(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId) {
        return ApiResponse.ok(photoService.getSyncHistory(new GetSyncHistoryQuery(albumId)));
    }

    @Operation(
            summary = "사진 메모 & 회상 태깅 [F1-04]",
            description = """
                    사진에 시기·장소·인물·추억 메모를 기록합니다.

                    - **메모**: 최대 200자
                    - **인물 태그**: 최대 10명
                    - 메모 변경 시 AI 회상 콘텐츠 재생성이 자동으로 트리거됩니다.
                    - 모든 필드가 선택 사항입니다 (부분 업데이트 가능).

                    **시기 예시:** `"1978년 여름"`, `"2003년 봄"`
                    **메모 예시:** `"딸이랑 오리백숙 먹었다. 슴슴허니 맛이 좋~았다."`
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "메모 200자 초과 / 인물 태그 10명 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "앨범 또는 사진 없음")
    })
    @PatchMapping("/{photoId}/memo")
    public ApiResponse<PhotoResult> updateMemo(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "사진 UUID") @PathVariable String photoId,
            @Valid @RequestBody UpdatePhotoMemoRequest request) {

        List<UpdatePhotoMemoCommand.PersonTagItem> tags = request.personTags() == null ? List.of() :
                request.personTags().stream()
                        .map(t -> new UpdatePhotoMemoCommand.PersonTagItem(t.memberId(), t.memberName()))
                        .toList();

        PhotoResult result = photoService.updatePhotoMemo(new UpdatePhotoMemoCommand(
                albumId, photoId,
                request.timePeriod(), request.locationText(), request.memo(),
                tags
        ));
        return ApiResponse.ok(result, "메모가 저장되었습니다.");
    }

    @Operation(
            summary = "사진 삭제",
            description = """
                    사진을 삭제합니다.

                    **권한 규칙:**
                    - 앨범 소유자: 모든 사진 삭제 가능
                    - 일반 멤버: 본인이 업로드한 사진만 삭제 가능
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 또는 사진 없음")
    })
    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePhoto(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "사진 UUID") @PathVariable String photoId,
            @Parameter(description = "요청자 memberId", required = true)
            @RequestParam String requestingMemberId) {
        photoService.removePhoto(new RemovePhotoCommand(albumId, photoId, requestingMemberId));
    }

    private String computeHash(MultipartFile file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(file.getOriginalFilename().hashCode());
        }
    }
}
