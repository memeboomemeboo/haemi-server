package com.memeboo2.haemi.m2.presentation.exception;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.domain.model.post.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.memeboo2.haemi.m2")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class M2ExceptionHandler {

    @ExceptionHandler(MemoryPostNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handlePostNotFound(MemoryPostNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(EmptyPostContentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleEmptyContent(EmptyPostContentException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ForbiddenWordException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleForbiddenWord(ForbiddenWordException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PostTextTooLongException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTextTooLong(PostTextTooLongException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(TooManyPhotosException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTooManyPhotos(TooManyPhotosException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AlreadyRepliedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleAlreadyReplied(AlreadyRepliedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ReplyContentTooLongException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleReplyTooLong(ReplyContentTooLongException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(EmptyReplyContentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleEmptyReply(EmptyReplyContentException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({UnsupportedVoiceContentTypeException.class, VoiceInputTooLargeException.class,
            VoiceTranscriptTooLongException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidVoiceInput(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AiGenerationUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleAiGenerationUnavailable(AiGenerationUnavailableException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AiGenerationRateLimitedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleAiGenerationRateLimited(AiGenerationRateLimitedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AiGenerationRejectedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleAiGenerationRejected(AiGenerationRejectedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InvalidHeartEmojiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidHeartEmoji(InvalidHeartEmojiException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PostDeleteForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleDeleteForbidden(PostDeleteForbiddenException e) {
        return ApiResponse.error(e.getMessage());
    }
}
