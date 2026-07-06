package com.memeboo2.haemi.m5.presentation.exception;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m5.domain.model.care.AlarmTimeDuplicatedException;
import com.memeboo2.haemi.m5.domain.model.care.AlarmAccessDeniedException;
import com.memeboo2.haemi.m5.domain.model.care.AlarmNotAwaitingResponseException;
import com.memeboo2.haemi.m5.domain.model.care.VoiceAlarmNotFoundException;
import com.memeboo2.haemi.m5.domain.model.care.WalkCompletionUnavailableException;
import com.memeboo2.haemi.m5.domain.model.care.WalkRoutineNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.memeboo2.haemi.m5")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class M5ExceptionHandler {

    @ExceptionHandler({VoiceAlarmNotFoundException.class, WalkRoutineNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({
            AlarmTimeDuplicatedException.class,
            AlarmNotAwaitingResponseException.class,
            AlarmAccessDeniedException.class,
            WalkCompletionUnavailableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDuplicated(AlarmTimeDuplicatedException e) {
        return ApiResponse.error(e.getMessage());
    }
}
