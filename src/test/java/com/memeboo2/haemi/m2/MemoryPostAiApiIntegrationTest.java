package com.memeboo2.haemi.m2;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API가 Gemini 키 미설정 시 Stub 문구를 반환하지 않고 명시적으로 실패하는지 검증한다. */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        "haemi.ai.gemini.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemoryPostAiApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenPort tokenPort;
    @Autowired MemoryPostRepository posts;

    @Test
    void poemDraftReturns503InsteadOfAStubWhenGeminiIsNotConfigured() throws Exception {
        MemoryPost post = savePublishedPost();

        mockMvc.perform(get("/api/v1/albums/{albumId}/posts/{postId}/poem-draft", post.getAlbumId(), post.getPostId())
                        .header("Authorization", "Bearer " + token(MemberRole.FAMILY)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("AI 기능이 아직 설정되지 않았어요. 잠시 후 다시 시도해주세요."));
    }

    @Test
    void voiceReplyReturns503InsteadOfAStubWhenGeminiIsNotConfigured() throws Exception {
        MemoryPost post = savePublishedPost();
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                ("{\"elderId\":\"%s\",\"replyType\":\"VOICE\"}".formatted(UUID.randomUUID()))
                        .getBytes(StandardCharsets.UTF_8));
        MockMultipartFile voice = new MockMultipartFile("voice", "reply.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/albums/{albumId}/posts/{postId}/reply", post.getAlbumId(), post.getPostId())
                        .file(data)
                        .file(voice)
                        .header("Authorization", "Bearer " + token(MemberRole.ELDER)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("AI 기능이 아직 설정되지 않았어요. 잠시 후 다시 시도해주세요."));
    }

    private MemoryPost savePublishedPost() {
        MemoryPost post = MemoryPost.createDraft(UUID.randomUUID(),
                AuthorInfo.of(UUID.randomUUID().toString(), "가족", "딸"), "함께한 여름날", null, null);
        post.publish();
        return posts.save(post);
    }

    private String token(MemberRole role) {
        UUID memberId = UUID.randomUUID();
        return tokenPort.generateAccessToken(memberId, memberId + "@haemi.local", role);
    }
}
