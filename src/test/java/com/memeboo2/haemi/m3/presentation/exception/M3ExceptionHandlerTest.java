package com.memeboo2.haemi.m3.presentation.exception;

import com.memeboo2.haemi.m3.domain.model.training.TrainingPrerequisiteNotMetException;
import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestionGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class M3ExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new M3ExceptionHandler())
                .build();
    }

    @Test
    void prerequisiteFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/training-prerequisite"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5장 이상")));
    }

    @Test
    void generationFailureWithoutCacheReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/test/training-generation"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/training-prerequisite")
        void prerequisite() {
            throw TrainingPrerequisiteNotMetException.insufficientPhotos(4, 5);
        }

        @GetMapping("/test/training-generation")
        void generation() {
            throw new TrainingQuestionGenerationException(new IllegalStateException("unavailable"));
        }
    }
}
