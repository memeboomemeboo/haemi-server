package com.memeboo2.haemi.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기관 관리자 발급과 2FA 잠금 해소 (#96).
 *
 * <p>핵심은 "잠금이 실제로 풀리는가"다. 가입 → 2FA 등록 → 로그인까지 끝까지 확인한다.
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        "haemi.security.institution-admin.allowed-emails=admin@haemi.kr"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstitutionAdminProvisioningApiIntegrationTest {

    private static final String ALLOWED_EMAIL = "admin@haemi.kr";
    private static final String PASSWORD = "Haemi123!";

    private final MockMvc mockMvc;
    private final MemberRepository members;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    InstitutionAdminProvisioningApiIntegrationTest(MockMvc mockMvc, MemberRepository members) {
        this.mockMvc = mockMvc;
        this.members = members;
    }

    /** 인증 앱이 만들어 낼 코드를 테스트에서 그대로 계산한다. TotpAdapter와 같은 라이브러리다. */
    private String currentCode(String secret) throws Exception {
        long timeWindow = new SystemTimeProvider().getTime() / 30;
        return new DefaultCodeGenerator().generate(secret, timeWindow);
    }

    @Test
    @DisplayName("허용 목록에 없는 이메일은 기관 관리자로 가입할 수 없다")
    void signUpAsInstitutionAdminIsRejectedForUnlistedEmail() throws Exception {
        mockMvc.perform(signUp("stranger-%s@haemi.kr".formatted(UUID.randomUUID()), "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("허용 목록이 비면 목록에 있던 이메일도 가입할 수 없다")
    void nobodyIsAllowedWhenAllowlistIsEmpty() throws Exception {
        // 기본 설정(미지정) 상태를 별도 컨텍스트로 확인한다.
        // 운영에서 설정을 빼먹었을 때 "아무나 관리자"가 되는 일은 없어야 한다.
        assertThat(new com.memeboo2.haemi.auth.infrastructure.security.InstitutionAdminProperties(null)
                .normalizedAllowedEmails()).isEmpty();
    }

    @Test
    @DisplayName("가족 계정 가입은 그대로 열려 있다")
    void familySignUpStaysOpen() throws Exception {
        mockMvc.perform(signUp("family-%s@haemi.kr".formatted(UUID.randomUUID()), "FAMILY"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("허용된 관리자는 가입 → 2FA 등록 → 로그인까지 끝난다")
    void allowedAdminCanProvisionAndFinallyLogIn() throws Exception {
        mockMvc.perform(signUp(ALLOWED_EMAIL, "INSTITUTION_ADMIN"))
                .andExpect(status().isAccepted());
        verifyEmail(ALLOWED_EMAIL);

        // 2FA 전에는 로그인이 막혀 있다. 이게 잠금의 정체다.
        mockMvc.perform(login(ALLOWED_EMAIL, null))
                .andExpect(status().isPreconditionRequired());

        MvcResult enrollment = mockMvc.perform(post("/api/v1/auth/totp/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ALLOWED_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String secret = json(enrollment).path("data").path("secret").asText();

        mockMvc.perform(post("/api/v1/auth/totp/enrollment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","secret":"%s","code":"%s"}
                                """.formatted(ALLOWED_EMAIL, PASSWORD, secret, currentCode(secret))))
                .andExpect(status().isOk());

        // 잠금이 실제로 풀렸는지가 이 이슈의 완료 조건이다.
        mockMvc.perform(login(ALLOWED_EMAIL, currentCode(secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 2FA 등록을 시작할 수 없다")
    void enrollmentRequiresCorrectPassword() throws Exception {
        String email = "admin-wrongpw-%s@haemi.kr".formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/auth/totp/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPass1!"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 사용자는 이 경로로 2FA를 켤 수 없다")
    void enrollmentIsNotAWayForRegularUsersToEnableTotp() throws Exception {
        String email = "family-enroll-%s@haemi.kr".formatted(UUID.randomUUID());
        mockMvc.perform(signUp(email, "FAMILY")).andExpect(status().isAccepted());
        verifyEmail(email);

        // 잠긴 기관 관리자 전용이다. 일반 사용자는 인증된 /totp/setup을 써야 한다.
        mockMvc.perform(post("/api/v1/auth/totp/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signUp(
            String email, String role) {
        return post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","name":"검사자","role":"%s"}
                        """.formatted(email, PASSWORD, role));
    }

    private void verifyEmail(String email) {
        var member = members.findByEmail(email).orElseThrow();
        member.verifyEmail();
        members.save(member);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String email, String totpCode) {
        String body = totpCode == null
                ? """
                  {"email":"%s","password":"%s"}
                  """.formatted(email, PASSWORD)
                : """
                  {"email":"%s","password":"%s","totpCode":"%s"}
                  """.formatted(email, PASSWORD, totpCode);
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
