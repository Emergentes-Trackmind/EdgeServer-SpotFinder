package com.constructinsight.edgeserver.iot.application.service;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service: Device Management Operations
 * Handles device lifecycle operations like deletion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceManagementService {

    private final IotDeviceRepository deviceRepository;

    /**
     * Delete a device permanently by its serial number
     *
     * @param serialNumber Device serial number
     * @throws IllegalArgumentException if device not found
     */
    @Transactional
    public void deleteDevice(String serialNumber) {
        log.info("Attempting to delete device with serial number: {}", serialNumber);

        // Verify device exists before attempting deletion
        IotDevice device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + serialNumber));

        log.info("Device found: {} (ID: {}). Proceeding with deletion.", serialNumber, device.getId());

        deviceRepository.deleteBySerialNumber(serialNumber);

        log.info("Device {} successfully deleted from database", serialNumber);
    }
}

