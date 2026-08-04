package com.memeboo2.haemi.m0.presentation.exception;

import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.model.M0ConflictException;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.memeboo2.haemi.m0")
public class M0ExceptionHandler {

    @ExceptionHandler(M0NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(M0NotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(M0AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(M0AccessDeniedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(M0ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(M0ConflictException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(M0ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(M0ValidationException e) {
        return ApiResponse.error(e.getMessage());
    }
}
