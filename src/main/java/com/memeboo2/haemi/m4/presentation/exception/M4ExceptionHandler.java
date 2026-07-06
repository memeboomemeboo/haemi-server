package com.memeboo2.haemi.m4.presentation.exception;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveMetricNotFoundException;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReportNotFoundException;
import com.memeboo2.haemi.m4.domain.model.dashboard.DataInsufficientException;
import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientsNotConfiguredException;
import com.memeboo2.haemi.m4.domain.model.dashboard.InstitutionSeniorsNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.memeboo2.haemi.m4")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class M4ExceptionHandler {

    @ExceptionHandler(DataInsufficientException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataInsufficient(DataInsufficientException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AlertRecipientsNotConfiguredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleRecipientsNotConfigured(
            AlertRecipientsNotConfiguredException e
    ) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({
            CognitiveMetricNotFoundException.class,
            CognitiveReportNotFoundException.class,
            InstitutionSeniorsNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleMetricNotFound(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }
}
