package com.memeboo2.haemi.m3.presentation.exception;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m3.domain.model.training.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.memeboo2.haemi.m3")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class M3ExceptionHandler {

    @ExceptionHandler(TrainingSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(TrainingSessionNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({DailySessionAlreadyCompletedException.class, TrainingSessionAlreadyCompletedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleCompleted(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({GrandchildChanceExhaustedException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }
}
