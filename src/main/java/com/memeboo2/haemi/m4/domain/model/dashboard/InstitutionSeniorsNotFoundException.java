package com.memeboo2.haemi.m4.domain.model.dashboard;

public class InstitutionSeniorsNotFoundException extends RuntimeException {

    public InstitutionSeniorsNotFoundException(String institutionId) {
        super("소속 어르신을 등록해주세요. institutionId=" + institutionId);
    }
}
