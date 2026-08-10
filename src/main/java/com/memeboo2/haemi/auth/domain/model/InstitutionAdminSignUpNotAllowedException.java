package com.memeboo2.haemi.auth.domain.model;

/**
 * 허용 목록에 없는 이메일로 기관 관리자 가입을 시도했을 때 (#96).
 *
 * <p>공개 가입 엔드포인트가 권한 역할을 그대로 받으면, 아무나 기관 이메일로 계정을 선점해
 * 정당한 가입을 막을 수 있다. 관리자 발급은 운영이 설정으로 통제한다.
 */
public class InstitutionAdminSignUpNotAllowedException extends RuntimeException {

    public InstitutionAdminSignUpNotAllowedException() {
        super("기관 관리자 계정은 직접 가입할 수 없어요. 담당자에게 문의해 주세요.");
    }
}
