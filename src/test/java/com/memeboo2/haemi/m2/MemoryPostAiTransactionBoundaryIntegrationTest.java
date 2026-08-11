package com.memeboo2.haemi.m2;

import com.memeboo2.haemi.m2.application.command.ReplyToPostCommand;
import com.memeboo2.haemi.m2.application.query.GeneratePoemDraftQuery;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import com.memeboo2.haemi.m2.domain.port.AiPoemGeneratorPort;
import com.memeboo2.haemi.m2.domain.port.SttPort;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** AI 네트워크 호출은 DB 트랜잭션 안에서 실행되면 안 된다 (#98 회귀 방지). */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("test")
class MemoryPostAiTransactionBoundaryIntegrationTest {

    @Autowired MemoryPostApplicationService service;
    @Autowired MemoryPostRepository posts;

    @MockitoBean AiPoemGeneratorPort poemGenerator;
    @MockitoBean SttPort sttPort;

    @Test
    void poemGenerationRunsAfterTheReadTransactionHasClosed() {
        MemoryPost post = savePublishedPost();
        AtomicBoolean transactionActiveDuringAiCall = new AtomicBoolean(true);
        when(poemGenerator.generatePoem(anyString())).thenAnswer(invocation -> {
            transactionActiveDuringAiCall.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "시 초안";
        });

        String poem = service.generatePoemDraft(new GeneratePoemDraftQuery(post.getPostId().toString()));

        assertThat(poem).isEqualTo("시 초안");
        assertThat(transactionActiveDuringAiCall).isFalse();
    }

    @Test
    void voiceTranscriptionRunsOutsideTheReplyPersistenceTransaction() {
        MemoryPost post = savePublishedPost();
        AtomicBoolean transactionActiveDuringSttCall = new AtomicBoolean(true);
        when(sttPort.transcribe(any(), anyString(), anyString())).thenAnswer(invocation -> {
            transactionActiveDuringSttCall.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "고마워요";
        });

        service.replyToPost(new ReplyToPostCommand(post.getPostId().toString(), UUID.randomUUID().toString(),
                ReplyType.VOICE, null, new ByteArrayInputStream(new byte[]{1}), "audio/mpeg", "reply.mp3"));

        assertThat(transactionActiveDuringSttCall).isFalse();
        assertThat(posts.findById(post.getPostId()).orElseThrow().getElderReply().getContent()).isEqualTo("고마워요");
    }

    private MemoryPost savePublishedPost() {
        MemoryPost post = MemoryPost.createDraft(UUID.randomUUID(),
                AuthorInfo.of(UUID.randomUUID().toString(), "가족", "딸"), "함께한 여름날", null, null);
        post.publish();
        return posts.save(post);
    }
}
