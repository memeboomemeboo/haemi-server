package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;
import com.memeboo2.haemi.m0.domain.model.DeviceCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaDeviceCommandRepository extends JpaRepository<DeviceCommand, UUID> {

    List<DeviceCommand> findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            DeviceCommandStatus status, LocalDateTime nextAttemptAt);
}
