package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.OwnershipTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaOwnershipTransferRepository extends JpaRepository<OwnershipTransfer, UUID> {
}
