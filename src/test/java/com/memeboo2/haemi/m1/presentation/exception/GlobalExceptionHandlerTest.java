package com.memeboo2.haemi.m1.presentation.exception;

import com.memeboo2.haemi.common.exception.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 도메인_검증_예외는_메시지와_함께_400으로_나간다() throws Exception {
        mockMvc.perform(get("/test/domain-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("야간 차단 시간대는 0~23시여야 합니다."));
    }

    /** #99: 일괄 400 매핑이 사라졌으니 진짜 서버 버그는 500 통계에 잡혀야 한다. */
    @Test
    void 의도하지_않은_IllegalArgumentException은_500으로_떨어진다() throws Exception {
        mockMvc.perform(get("/test/unexpected-illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    /** 내부 예외 메시지가 그대로 밖으로 나가지 않는지 확인한다. */
    @Test
    void 내부_예외_메시지는_응답에_실리지_않는다() throws Exception {
        mockMvc.perform(get("/test/unexpected-illegal-argument"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Invalid UUID string"))));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/domain-validation")
        void domainValidation() {
            throw new DomainValidationException("야간 차단 시간대는 0~23시여야 합니다.");
        }

        @GetMapping("/test/unexpected-illegal-argument")
        void unexpectedIllegalArgument() {
            throw new IllegalArgumentException("Invalid UUID string: 내부-구현-세부사항");
        }
    }
}
