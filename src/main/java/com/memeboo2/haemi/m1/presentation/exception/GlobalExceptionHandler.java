package com.memeboo2.haemi.m1.presentation.exception;

import com.memeboo2.haemi.m1.application.service.AlbumNotFoundException;
import com.memeboo2.haemi.m1.application.service.ReminiscenceContentNotFoundException;
import com.memeboo2.haemi.m1.domain.model.album.*;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlbumNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleAlbumNotFound(AlbumNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PhotoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handlePhotoNotFound(PhotoNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ReminiscenceContentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleContentNotFound(ReminiscenceContentNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(DuplicatePhotoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicatePhoto(DuplicatePhotoException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(UnsupportedPhotoFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnsupportedFormat(UnsupportedPhotoFormatException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PhotoFileSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleFileSizeExceeded(PhotoFileSizeExceededException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MemoLengthExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMemoLength(MemoLengthExceededException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PersonTagLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTagLimit(PersonTagLimitExceededException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AlbumMemberLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMemberLimit(AlbumMemberLimitExceededException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(PhotoDeleteForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleDeleteForbidden(PhotoDeleteForbiddenException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.error(message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception e) {
        log.error("예상치 못한 오류", e);
        return ApiResponse.error("서버 오류가 발생했습니다.");
    }
}
