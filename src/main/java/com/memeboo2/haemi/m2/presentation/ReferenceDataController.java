package com.memeboo2.haemi.m2.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.domain.model.post.HeartEmoji;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "M2-Reference", description = "마음 이모지 등 참조 데이터")
@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceDataController {

    @Operation(
            summary = "마음 이모지 목록 조회",
            description = "어르신 답변에 사용할 수 있는 마음 이모지 목록을 반환합니다. 클라이언트는 디자인 하드코딩이 아닌 이 API 값을 따릅니다."
    )
    @GetMapping("/heart-emojis")
    public ApiResponse<List<HeartEmojiResponse>> getHeartEmojis() {
        List<HeartEmojiResponse> list = Arrays.stream(HeartEmoji.values())
                .map(e -> new HeartEmojiResponse(e.name(), e.getCode(), e.getLabel()))
                .toList();
        return ApiResponse.ok(list);
    }

    public record HeartEmojiResponse(String key, String code, String label) {}
}
