package com.memeboo2.haemi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #100 — API_DOCS_ENABLED 토글이 실제로 문서 노출을 여닫는지 확인한다.
 *
 * <p>기본값은 켜짐이다. 클라이언트 개발이 끝난 뒤 배포 설정만 바꿔서 끌 수 있어야 한다.
 */
class ApiDocsToggleIntegrationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("기본값(켬)")
    class Enabled {

        @Autowired
        MockMvc mockMvc;

        @Test
        @DisplayName("API 문서는 인증 없이 열린다")
        void apiDocsArePublic() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "springdoc.api-docs.enabled=false",
            "springdoc.swagger-ui.enabled=false"
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("끔")
    class Disabled {

        @Autowired
        MockMvc mockMvc;

        /** 200이 아니기만 하면 된다. 끄면 핸들러 자체가 사라지므로 permitAll도 함께 빠진다. */
        @Test
        @DisplayName("API 문서가 더 이상 응답하지 않는다")
        void apiDocsAreGone() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(result -> {
                        int actualStatus = result.getResponse().getStatus();
                        if (actualStatus == 200) {
                            throw new AssertionError("문서를 껐는데도 /v3/api-docs가 200을 돌려준다");
                        }
                    });
        }

        @Test
        @DisplayName("Swagger UI도 함께 닫힌다")
        void swaggerUiIsGone() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(result -> {
                        int actualStatus = result.getResponse().getStatus();
                        if (actualStatus == 200) {
                            throw new AssertionError("문서를 껐는데도 Swagger UI가 200을 돌려준다");
                        }
                    });
        }
    }
}
