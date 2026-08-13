package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DeviceCommandRepository {

    DeviceCommand save(DeviceCommand command);

    List<DeviceCommand> findPendingBefore(LocalDateTime now);
    List<DeviceCommand> findPendingByElderId(UUID elderId);
}
