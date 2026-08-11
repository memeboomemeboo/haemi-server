package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceCommandRepository {

    DeviceCommand save(DeviceCommand command);

    List<DeviceCommand> findPendingBefore(LocalDateTime now);
}
