package com.memeboo2.haemi.auth.domain.model;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String message) {
        super(message);
    }
}
