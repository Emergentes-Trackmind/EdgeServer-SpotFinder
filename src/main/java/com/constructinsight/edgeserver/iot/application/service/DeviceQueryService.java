package com.constructinsight.edgeserver.iot.application.service;

import com.constructinsight.edgeserver.iot.application.dto.DeviceKpiDto;
import com.constructinsight.edgeserver.iot.domain.model.DeviceStatus;
import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Service: Device Query Operations
 * All queries enforce data privacy by filtering on ownerId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceQueryService {

    private final IotDeviceRepository deviceRepository;
    private static final int LOW_BATTERY_THRESHOLD = 20;

    /**
     * CRITICAL: Find all devices owned by a specific user
     * This is the primary privacy filter - only returns user's devices
     *
     * @param userId User identifier
     * @return List of devices owned by the user
     */
    @Transactional(readOnly = true)
    public List<IotDevice> findAllByUser(String userId) {
        log.debug("Querying devices for user: {}", userId);
        List<IotDevice> devices = deviceRepository.findAllByOwnerId(userId);
        log.debug("Found {} devices for user {}", devices.size(), userId);
        return devices;
    }

    /**
     * Calculate KPIs for a specific user's devices
     * PRIVACY: Only calculates stats for devices owned by userId
     *
     * @param userId User identifier
     * @return KPI statistics
     */
    @Transactional(readOnly = true)
    public DeviceKpiDto getKpis(String userId) {
        log.debug("Calculating KPIs for user: {}", userId);

        List<IotDevice> userDevices = deviceRepository.findAllByOwnerId(userId);

        if (userDevices.isEmpty()) {
            return DeviceKpiDto.builder()
                    .totalDevices(0)
                    .onlineDevices(0)
                    .offlineDevices(0)
                    .averageBattery(0.0)
                    .lowBatteryDevices(0)
                    .build();
        }

        long totalDevices = userDevices.size();
        long onlineDevices = userDevices.stream()
                .filter(d -> d.getStatus() == DeviceStatus.ONLINE)
                .count();
        long offlineDevices = userDevices.stream()
                .filter(d -> d.getStatus() == DeviceStatus.OFFLINE)
                .count();
        double averageBattery = userDevices.stream()
                .mapToInt(IotDevice::getBattery)
                .average()
                .orElse(0.0);
        long lowBatteryDevices = userDevices.stream()
                .filter(d -> d.getBattery() < LOW_BATTERY_THRESHOLD)
                .count();

        DeviceKpiDto kpis = DeviceKpiDto.builder()
                .totalDevices(totalDevices)
                .onlineDevices(onlineDevices)
                .offlineDevices(offlineDevices)
                .averageBattery(Math.round(averageBattery * 100.0) / 100.0)
                .lowBatteryDevices(lowBatteryDevices)
                .build();

        log.debug("KPIs for user {}: {}", userId, kpis);
        return kpis;
    }
}

