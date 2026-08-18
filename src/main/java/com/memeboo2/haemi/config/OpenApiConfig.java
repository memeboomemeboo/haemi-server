package com.memeboo2.haemi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI haemiOpenAPI(Environment environment) {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("해미 API")
                        .description("""
                                **해미** — 기억과 가족을 잇는 AI 회상 치료 플랫폼

                                치매 어르신의 장기 기억을 자극하는 AI 개인화 회상 콘텐츠와 가족 참여형 디지털 추억 앨범을 통해,
                                인지 기능 유지와 가족의 정서적 유대를 동시에 지원합니다.

                                ## 모듈 구성
                                | 모듈 | 설명 |
                                |------|------|
                                | **M0 계정·어르신 기반** | 계정·가족 그룹, 어르신 프로필, 접근 모드, 인물 마스터, 상태 관리 |
                                | **M1 기억 회상 갤러리** | 사진·음성 업로드 → AI 개인화 회상 콘텐츠 자동 생성 |
                                | M2 가족 추억글 시스템 | 가족 추억 글·사진 등록 ↔ 어르신 비동기 답변 |
                                | M3 AI 인지 훈련 | 난이도 적응형 퀴즈·퍼즐 |
                                | M4 인지 변화 추적 | 활동 데이터 시각화 리포트 · 기관 담당자 포털 |
                                | M5 공통 알림 | 손주 목소리 알람 등 부가 기능 |

                                태그별 상세 설명은 각 태그 헤더에 표시됩니다.
                                """)
                        .version("v2.0")
                        .contact(new Contact()
                                .name("memeboo2")
                                .email("aa01034795025@gmail.com"))
                        .license(new License().name("Private")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 액세스 토큰. 로그인 후 발급받은 accessToken을 입력하세요.")));

        List<Server> servers = openApiServers(environment);
        if (!servers.isEmpty()) {
            openAPI.servers(servers);
        }
        return openAPI;
    }

    private List<Server> openApiServers(Environment environment) {
        String serverUrl = environment.getProperty("haemi.openapi.server-url", "");
        String localServerUrl = environment.getProperty("haemi.openapi.local-server-url", "http://localhost:8080");
        boolean includeLocalServer = environment.getProperty(
                "haemi.openapi.include-local-server",
                Boolean.class,
                true
        );

        List<Server> servers = new ArrayList<>();
        if (StringUtils.hasText(serverUrl) && !isLocalhostUrl(serverUrl)) {
            servers.add(new Server().url(serverUrl).description("운영 서버"));
        }
        if (includeLocalServer && StringUtils.hasText(localServerUrl)) {
            servers.add(new Server().url(localServerUrl).description("로컬 개발 서버"));
        }
        return servers;
    }

    private boolean isLocalhostUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            return host.equalsIgnoreCase("localhost")
                    || host.equals("127.0.0.1")
                    || host.equals("0.0.0.0")
                    || host.equals("::1");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
