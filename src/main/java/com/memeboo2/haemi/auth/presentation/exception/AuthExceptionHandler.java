package com.memeboo2.haemi.auth.presentation.exception;

import com.memeboo2.haemi.auth.domain.model.*;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.memeboo2.haemi.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateEmail(DuplicateEmailException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidCredentials(InvalidCredentialsException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InstitutionAdminSignUpNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleInstitutionAdminSignUp(InstitutionAdminSignUpNotAllowedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AccountSuspendedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleSuspended(AccountSuspendedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(TotpRequiredException.class)
    @ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
    public ApiResponse<Void> handleTotpRequired(TotpRequiredException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(MemberNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AlreadyWithdrawnException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiResponse<Void> handleWithdrawn(AlreadyWithdrawnException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({InvalidEmailException.class, InvalidNameException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }
}
