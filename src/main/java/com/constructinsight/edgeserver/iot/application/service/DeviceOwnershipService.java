package com.constructinsight.edgeserver.iot.application.service;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service: Device Ownership Management
 * Handles binding/unbinding logic with strict privacy controls
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceOwnershipService {

    private final IotDeviceRepository deviceRepository;

    /**
     * Bind a device to a user (establish ownership)
     *
     * @param serialNumber Device serial number
     * @param userId User identifier
     * @throws IllegalArgumentException if device not found or already bound
     */
    @Transactional
    public IotDevice bindDevice(String serialNumber, String userId) {
        log.info("Binding device {} to user {}", serialNumber, userId);

        IotDevice device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + serialNumber));

        if (device.isBound() && !device.isBoundToUser(userId)) {
            throw new IllegalStateException("Device is already bound to another user");
        }

        device.bind(userId);
        IotDevice savedDevice = deviceRepository.save(device);

        log.info("Device {} successfully bound to user {}", serialNumber, userId);
        return savedDevice;
    }

    /**
     * Unbind a device from its current owner
     * CRITICAL: Only the owner can unbind their device (Privacy Control)
     *
     * @param serialNumber Device serial number
     * @param userId User identifier (must match device owner)
     * @throws IllegalArgumentException if device not found
     * @throws SecurityException if user is not the owner
     */
    @Transactional
    public IotDevice unbindDevice(String serialNumber, String userId) {
        log.info("Unbinding device {} from user {}", serialNumber, userId);

        IotDevice device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + serialNumber));

        // PRIVACY CHECK: Only the owner can unbind
        if (!device.isBoundToUser(userId)) {
            throw new SecurityException("User " + userId + " is not authorized to unbind this device");
        }

        device.unbind();
        IotDevice savedDevice = deviceRepository.save(device);

        log.info("Device {} successfully unbound from user {}", serialNumber, userId);
        return savedDevice;
    }
}

