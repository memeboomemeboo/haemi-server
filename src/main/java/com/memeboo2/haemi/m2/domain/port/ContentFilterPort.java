package com.memeboo2.haemi.m2.domain.port;

public interface ContentFilterPort {

    /**
     * 금칙어·혐오 표현 포함 여부를 검사한다.
     * @return true이면 게시 불가 콘텐츠
     */
    boolean containsForbiddenWord(String text);
}
