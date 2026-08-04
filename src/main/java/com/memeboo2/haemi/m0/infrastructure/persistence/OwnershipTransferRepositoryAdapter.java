package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.OwnershipTransfer;
import com.memeboo2.haemi.m0.domain.repository.OwnershipTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OwnershipTransferRepositoryAdapter implements OwnershipTransferRepository {

    private final JpaOwnershipTransferRepository transfers;

    @Override
    public OwnershipTransfer save(OwnershipTransfer transfer) {
        return transfers.save(transfer);
    }

    @Override
    public Optional<OwnershipTransfer> findById(UUID transferId) {
        return transfers.findById(transferId);
    }
}
