package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;
import com.memeboo2.haemi.m0.domain.model.DeviceCommandStatus;
import com.memeboo2.haemi.m0.domain.repository.DeviceCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeviceCommandRepositoryAdapter implements DeviceCommandRepository {

    private final JpaDeviceCommandRepository commands;

    @Override
    public DeviceCommand save(DeviceCommand command) {
        return commands.save(command);
    }

    @Override
    public List<DeviceCommand> findPendingBefore(LocalDateTime now) {
        return commands.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                DeviceCommandStatus.PENDING, now);
    }
}
