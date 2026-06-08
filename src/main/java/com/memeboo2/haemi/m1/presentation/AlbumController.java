package com.memeboo2.haemi.m1.presentation;

import com.memeboo2.haemi.m1.application.command.CreateAlbumCommand;
import com.memeboo2.haemi.m1.application.command.InviteMemberCommand;
import com.memeboo2.haemi.m1.application.dto.AlbumResult;
import com.memeboo2.haemi.m1.application.dto.TimelineResult;
import com.memeboo2.haemi.m1.application.query.GetAlbumQuery;
import com.memeboo2.haemi.m1.application.query.GetTimelineQuery;
import com.memeboo2.haemi.m1.application.service.AlbumApplicationService;
import com.memeboo2.haemi.m1.presentation.dto.request.CreateAlbumRequest;
import com.memeboo2.haemi.m1.presentation.dto.request.InviteMemberRequest;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "M1-Album", description = "F1-03 가족 공동 기억 앨범 / F1-06 연관 이미지 타임라인")
@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumApplicationService albumService;

    @Operation(
            summary = "앨범 생성 [F1-03]",
            description = """
                    어르신 프로필과 가족 그룹을 연결하는 기억 앨범을 생성합니다.
                    앨범 소유자(ownerMemberId)가 자동으로 첫 번째 멤버로 추가됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "앨범 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 항목 누락",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AlbumResult> createAlbum(@Valid @RequestBody CreateAlbumRequest request) {
        AlbumResult result = albumService.createAlbum(new CreateAlbumCommand(
                request.elderProfileId(), request.groupId(), request.ownerMemberId()
        ));
        return ApiResponse.ok(result, "앨범이 생성되었습니다.");
    }

    @Operation(
            summary = "앨범 조회 [F1-03]",
            description = "앨범 상세 정보와 최근 사진 10장을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @GetMapping("/{albumId}")
    public ApiResponse<AlbumResult> getAlbum(
            @Parameter(description = "앨범 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String albumId) {
        return ApiResponse.ok(albumService.getAlbum(new GetAlbumQuery(albumId)));
    }

    @Operation(
            summary = "멤버 초대 [F1-03]",
            description = """
                    가족 그룹에 새 멤버를 초대합니다.
                    - 최대 10명 제한
                    - 초대 시 대상 멤버에게 푸시 알림 발송
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "초대 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹 정원(10명) 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @PostMapping("/{albumId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> inviteMember(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Valid @RequestBody InviteMemberRequest request) {
        albumService.inviteMember(new InviteMemberCommand(albumId, request.inviterId(), request.inviteeId()));
        return ApiResponse.ok(null, "초대되었습니다.");
    }

    @Operation(
            summary = "타임라인 조회 [F1-06]",
            description = """
                    인물·장소 필터로 사진을 연도·계절 단위로 그루핑한 타임라인을 반환합니다.
                    - 날짜 정보가 없는 사진은 `날짜 미상` 그룹으로 마지막에 표시됩니다.
                    - `role=ELDER` : 슬라이드형 뷰 기준 (프론트에서 활용)
                    - `role=FAMILY` : 그리드 편집 뷰 기준
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @GetMapping("/{albumId}/timeline")
    public ApiResponse<TimelineResult> getTimeline(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "특정 인물 필터 (memberId)") @RequestParam(required = false) String memberId,
            @Parameter(description = "장소 키워드 필터") @RequestParam(required = false) String location,
            @Parameter(description = "뷰어 역할", schema = @Schema(allowableValues = {"ELDER", "FAMILY"}))
            @RequestParam(defaultValue = "FAMILY") String role) {
        TimelineResult result = albumService.getTimeline(
                new GetTimelineQuery(albumId, memberId, location, role));
        return ApiResponse.ok(result);
    }
}
