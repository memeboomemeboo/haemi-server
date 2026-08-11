package com.memeboo2.haemi.m1.presentation.exception;

import com.memeboo2.haemi.m1.application.service.AlbumNotFoundException;
import com.memeboo2.haemi.m1.application.service.ReminiscenceContentNotFoundException;
import com.memeboo2.haemi.m1.domain.model.album.*;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryAccessDeniedException;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryContentRequiredException;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationException;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryValidationException;
import com.memeboo2.haemi.m1.application.service.MemoryNotFoundException;
import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
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

    @ExceptionHandler(MemoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleMemoryNotFound(MemoryNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(M0NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleM0NotFound(M0NotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(M0AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleM0AccessDenied(M0AccessDeniedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MemoryAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleMemoryAccessDenied(MemoryAccessDeniedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MemoryModerationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleMemoryModeration(MemoryModerationException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({MemoryContentRequiredException.class, MemoryValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMemoryValidation(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(DuplicatePhotoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicatePhoto(DuplicatePhotoException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AlbumAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleAlbumAlreadyExists(AlbumAlreadyExistsException e) {
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

    @ExceptionHandler(AlbumAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAlbumAccessDenied(AlbumAccessDeniedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MemberNotInvitedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleMemberNotInvited(MemberNotInvitedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InviteExpiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInviteExpired(InviteExpiredException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(SyncConditionNotMetException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleSyncConditionNotMet(SyncConditionNotMetException e) {
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

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return ApiResponse.error(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadableMessage(HttpMessageNotReadableException e) {
        return ApiResponse.error("요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "필요한 타입";
        return ApiResponse.error("%s 값이 올바르지 않습니다. 기대 타입: %s".formatted(e.getName(), requiredType));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        return ApiResponse.error("필수 요청 파라미터가 누락되었습니다: " + e.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingPart(MissingServletRequestPartException e) {
        return ApiResponse.error("필수 multipart 항목이 누락되었습니다: " + e.getRequestPartName());
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMultipart(MultipartException e) {
        return ApiResponse.error("multipart 요청 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ApiResponse.error("업로드 파일 크기가 제한을 초과했습니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        return ApiResponse.error("요청한 리소스를 찾을 수 없습니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage() != null ? e.getMessage() : "요청 값이 올바르지 않습니다.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception e) {
        log.error("예상치 못한 오류", e);
        return ApiResponse.error("서버 오류가 발생했습니다.");
    }
}
