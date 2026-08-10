package com.memeboo2.haemi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
