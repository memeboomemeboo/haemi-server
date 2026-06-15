package com.memeboo2.haemi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI haemiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("해미 API")
                        .description("""
                                **해미** — 기억과 가족을 잇는 AI 회상 치료 플랫폼

                                치매 어르신의 장기 기억을 자극하는 AI 개인화 회상 콘텐츠와 가족 참여형 디지털 추억 앨범을 통해,
                                인지 기능 유지와 가족의 정서적 유대를 동시에 지원합니다.

                                ## 모듈 구성
                                | 모듈 | 설명 |
                                |------|------|
                                | **M1 기억 회상 갤러리** | 사진·음성 업로드 → AI 개인화 회상 콘텐츠 자동 생성 |
                                | M2 가족 추억글 시스템 | 가족 추억 글·사진 등록 ↔ 어르신 비동기 답변 |
                                | M3 AI 인지 훈련 | 난이도 적응형 퀴즈·퍼즐 |
                                | M4 인지 변화 추적 | 활동 데이터 시각화 리포트 |
                                | M5 공통 알림 | 손주 목소리 알람 등 부가 기능 |
                                """)
                        .version("v2.0")
                        .contact(new Contact()
                                .name("memeboo2")
                                .email("aa01034795025@gmail.com"))
                        .license(new License().name("Private")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버"),
                        new Server().url("https://api.haemi.kr").description("운영 서버")
                ))
                .tags(List.of(
                        new Tag().name("M1-Album").description("F1-03 가족 공동 기억 앨범 / F1-06 연관 이미지 타임라인"),
                        new Tag().name("M1-Photo").description("F1-01 사진 개별 저장 / F1-02 일괄 동기화 / F1-04 메모 & 회상 태깅"),
                        new Tag().name("M1-Reminiscence").description("F1-05 AI 회상 콘텐츠 자동 생성")
                ))
                .components(new Components());
    }
}
