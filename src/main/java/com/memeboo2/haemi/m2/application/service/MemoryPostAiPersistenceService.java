package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m2.application.dto.MemoryPostResult;
import com.memeboo2.haemi.m2.domain.model.post.AlreadyRepliedException;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostNotFoundException;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 AI 호출 전후에 필요한 짧은 DB 작업만 별도 트랜잭션으로 수행한다.
 * AI 호출 중에는 DB 커넥션을 점유하지 않도록 워크플로 서비스와 분리한다.
 */
@Service
@RequiredArgsConstructor
public class MemoryPostAiPersistenceService {

    private final MemoryPostRepository postRepository;

    @Transactional(readOnly = true)
    public void verifyCanReply(String postId) {
        if (loadPostOrThrow(postId).hasElderReply()) {
            throw new AlreadyRepliedException();
        }
    }

    @Transactional
    public MemoryPostResult saveReply(String postId, ReplyType replyType, String content) {
        MemoryPost post = loadPostOrThrow(postId);
        post.submitElderReply(replyType, content);
        postRepository.save(post);
        return MemoryPostResult.from(post);
    }

    @Transactional(readOnly = true)
    public String loadPoemSource(String postId) {
        MemoryPost post = loadPostOrThrow(postId);
        return post.getTextContent() != null ? post.getTextContent() : "소중한 추억";
    }

    private MemoryPost loadPostOrThrow(String postId) {
        return postRepository.findById(MemoryPostId.of(postId))
                .orElseThrow(() -> new MemoryPostNotFoundException(postId));
    }
}
