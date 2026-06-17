package com.memeboo2.haemi.auth.domain.repository;

import com.memeboo2.haemi.auth.domain.model.Member;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(UUID id);

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
