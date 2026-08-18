package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.command.CreatePersonCommand;
import com.memeboo2.haemi.m0.application.command.TagPhotoPersonCommand;
import com.memeboo2.haemi.m0.application.command.UpdatePersonCommand;
import com.memeboo2.haemi.m0.application.dto.PersonResult;
import com.memeboo2.haemi.m0.application.service.PersonApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.CreatePersonRequest;
import com.memeboo2.haemi.m0.presentation.dto.request.TagPhotoPersonRequest;
import com.memeboo2.haemi.m0.presentation.dto.request.UpdatePersonRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "M0-Person", description = "F0-04 인물 마스터와 어르신 노출 제어")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class PersonController {

    private final PersonApplicationService persons;

    @Operation(summary = "인물 등록", description = "생존 여부를 반드시 기록하며 기본 노출은 SHOWN입니다.")
    @PostMapping("/groups/{groupId}/persons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PersonResult> create(@AuthenticationPrincipal AuthenticatedMember member,
                                            @PathVariable UUID groupId,
                                            @Valid @RequestBody CreatePersonRequest request) {
        return ApiResponse.ok(persons.create(member.memberId(), groupId,
                new CreatePersonCommand(request.name(), request.relation(), request.lifeStatus(),
                        request.deceasedAt(), request.visibility(), request.nickname(), request.profilePhotoId(),
                        request.linkedMemberId())));
    }

    @Operation(summary = "인물 생존·노출 상태 변경", description = "대표 보호자만 변경할 수 있고, 변경 즉시 콘텐츠 무효화 이벤트를 발행합니다.")
    @PatchMapping("/persons/{personId}")
    public ApiResponse<PersonResult> update(@AuthenticationPrincipal AuthenticatedMember member,
                                            @PathVariable UUID personId,
                                            @Valid @RequestBody UpdatePersonRequest request) {
        return ApiResponse.ok(persons.update(member.memberId(), personId,
                new UpdatePersonCommand(request.relation(), request.lifeStatus(), request.deceasedAt(),
                        request.visibility(), request.nickname(), request.profilePhotoId())));
    }

    @Operation(summary = "인물 숨김/비활성화", description = "사진 태그는 보존하고 이후 어르신 노출만 차단합니다.")
    @DeleteMapping("/persons/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal AuthenticatedMember member, @PathVariable UUID personId) {
        persons.deactivate(member.memberId(), personId);
    }

    @Operation(summary = "사진-인물 태깅", description = "확인되지 않은 AI 태그(confidence < 0.7)는 인물명을 콘텐츠에 사용하지 않습니다.")
    @PostMapping("/photos/{photoId}/persons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> tagPhoto(@AuthenticationPrincipal AuthenticatedMember member,
                                      @PathVariable UUID photoId,
                                      @Valid @RequestBody TagPhotoPersonRequest request) {
        persons.tagPhoto(member.memberId(), photoId,
                new TagPhotoPersonCommand(request.personId(), request.confidence(), request.confirmed()));
        return ApiResponse.ok(null);
    }

    @Operation(summary = "노출 가능 인물 조회")
    @GetMapping("/groups/{groupId}/persons")
    public ApiResponse<List<PersonResult>> findShown(@AuthenticationPrincipal AuthenticatedMember member,
                                                     @PathVariable UUID groupId,
                                                     @RequestParam(defaultValue = "SHOWN") String visibility) {
        if (!"SHOWN".equalsIgnoreCase(visibility)) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(persons.findShown(member.memberId(), groupId));
    }
}
