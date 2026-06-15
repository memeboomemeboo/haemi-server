package com.memeboo2.haemi.m2.domain.port;

public interface AiPoemGeneratorPort {

    /**
     * 추억글 본문을 기반으로 어르신이 답변할 시(詩) 초안을 생성한다.
     * @param postText 추억글 원문
     * @return 시 초안 텍스트 (최대 200자)
     */
    String generatePoem(String postText);
}
