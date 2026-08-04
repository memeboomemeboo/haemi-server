package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.OwnershipTransfer;

import java.util.Optional;
import java.util.UUID;

public interface OwnershipTransferRepository {
    OwnershipTransfer save(OwnershipTransfer transfer);
    Optional<OwnershipTransfer> findById(UUID transferId);
}
