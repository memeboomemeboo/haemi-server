package com.memeboo2.haemi.auth.infrastructure.persistence;

import com.memeboo2.haemi.auth.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaMemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
