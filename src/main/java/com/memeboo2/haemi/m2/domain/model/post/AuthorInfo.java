package com.memeboo2.haemi.m2.domain.model.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorInfo {

    @Column(name = "author_member_id", nullable = false)
    private String memberId;

    @Column(name = "author_name", nullable = false)
    private String memberName;

    // 가족 관계 (아들, 딸, 손녀 등)
    @Column(name = "author_relation", nullable = false)
    private String relation;

    private AuthorInfo(String memberId, String memberName, String relation) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.relation = relation;
    }

    public static AuthorInfo of(String memberId, String memberName, String relation) {
        return new AuthorInfo(memberId, memberName, relation);
    }
}
