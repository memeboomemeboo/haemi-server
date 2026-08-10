package com.memeboo2.haemi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 개발 환경 전용 CORS 허용 (#80).
 *
 * <p>로컬에서 띄운 FCM 토큰 테스터 페이지(tools/fcm-token-tester)가 다른 포트에서 API를 호출하기 위한 설정이다.
 * 운영 프로필에서는 이 빈이 없고, 그때 SecurityConfig의 cors()는 아무 것도 하지 않는다.
 */
@Profile("dev")
@Configuration
public class DevCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 로컬 개발 서버만 허용한다. 자격증명 쿠키는 쓰지 않는다.
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
