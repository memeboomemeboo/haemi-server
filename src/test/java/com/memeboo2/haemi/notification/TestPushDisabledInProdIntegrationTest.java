package com.memeboo2.haemi.notification;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.config.DevCorsConfig;
import com.memeboo2.haemi.notification.presentation.TestPushController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * dev 전용 장치가 운영 표면에 새어나가지 않는지 확인한다 (#80).
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestPushDisabledInProdIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final ApplicationContext context;

    @Autowired
    TestPushDisabledInProdIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, ApplicationContext context) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.context = context;
    }

    @Test
    @DisplayName("dev 프로필이 아니면 테스트 발송 컨트롤러가 존재하지 않는다")
    void testSendControllerIsAbsent() {
        assertThat(context.getBeanNamesForType(TestPushController.class)).isEmpty();
    }

    @Test
    @DisplayName("dev 프로필이 아니면 테스트 발송 엔드포인트가 404다")
    void testSendEndpointIsNotFound() throws Exception {
        String jwt = tokenPort.generateAccessToken(UUID.randomUUID(), "someone@haemi.test", MemberRole.FAMILY);

        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("dev 프로필이 아니면 CORS 허용 설정도 없다")
    void devCorsIsAbsent() {
        assertThat(context.getBeanNamesForType(DevCorsConfig.class)).isEmpty();
        // MVC가 스스로 등록하는 mvcHandlerMappingIntrospector도 CorsConfigurationSource라
        // 타입만으로는 판별할 수 없다. 시큐리티가 이름으로 찾는 빈이 없는지를 본다.
        assertThat(context.containsBean("corsConfigurationSource")).isFalse();
    }
}
