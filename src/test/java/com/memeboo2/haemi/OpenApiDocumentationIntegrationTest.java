package com.memeboo2.haemi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "haemi.openapi.server-url=http://ec2.example.com:8080",
        "haemi.openapi.include-local-server=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    OpenApiDocumentationIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void apiDocsExposeConfiguredServerUrlOnly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers.length()").value(1))
                .andExpect(jsonPath("$.servers[0].url").value("http://ec2.example.com:8080"))
                .andExpect(jsonPath("$.servers[0].description").value("운영 서버"));
    }

    @Test
    void apiDocsDoNotExposeLegacyCognitiveScoreDashboard() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/cognitive-dashboard/institutions/{institutionId}']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/cognitive-dashboard/institutions/{institutionId}/export']").doesNotExist());
    }

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    /** 문서 없이 노출되는 엔드포인트가 다시 생기지 않도록 막는다. */
    @Test
    void everyOperationHasSummary() throws Exception {
        JsonNode paths = apiDocs().get("paths");

        List<String> missing = new ArrayList<>();
        paths.properties().forEach(path -> path.getValue().properties().forEach(operation -> {
            if (HTTP_METHODS.contains(operation.getKey()) && !operation.getValue().hasNonNull("summary")) {
                missing.add(operation.getKey().toUpperCase() + " " + path.getKey());
            }
        }));

        assertThat(missing).isEmpty();
    }

    /** 태그 이름은 유일해야 한다. 설정과 컨트롤러가 같은 태그를 서로 다르게 설명하면 중복으로 새어 나온다. */
    @Test
    void tagNamesAreUnique() throws Exception {
        List<String> names = apiDocs().get("tags").findValuesAsText("name");

        assertThat(names).doesNotHaveDuplicates();
    }

    /** SecurityConfig에서 permitAll인 경로는 문서에서도 인증 없이 호출 가능해야 한다. */
    @Test
    void publicEndpointsAreDocumentedWithoutSecurity() throws Exception {
        Set<String> publicOperations = new HashSet<>(Set.of(
                "post /api/v1/auth/signup",
                "post /api/v1/auth/login",
                "post /api/v1/auth/refresh",
                "get /api/v1/auth/email-verifications/confirm",
                "post /api/v1/auth/email-verifications/resend",
                "post /api/v1/auth/totp/enrollment",
                "post /api/v1/auth/totp/enrollment/verify",
                "post /api/v1/invitations/{code}/accept-elder",
                "post /api/v1/elder-sessions/refresh"
        ));
        JsonNode paths = apiDocs().get("paths");

        List<String> stillSecured = new ArrayList<>();
        paths.properties().forEach(path -> path.getValue().properties().forEach(operation -> {
            String key = operation.getKey() + " " + path.getKey();
            if (publicOperations.remove(key) && !operation.getValue().path("security").isEmpty()) {
                stillSecured.add(key);
            }
        }));

        assertThat(stillSecured).isEmpty();
        assertThat(publicOperations).as("문서에서 사라진 공개 엔드포인트").isEmpty();
    }

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body);
    }
}
