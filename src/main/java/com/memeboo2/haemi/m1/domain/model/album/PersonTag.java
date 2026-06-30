package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonTag {

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    private PersonTag(String memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;
    }

    public static PersonTag of(String memberId, String memberName) {
        return new PersonTag(memberId, memberName);
    }
}
