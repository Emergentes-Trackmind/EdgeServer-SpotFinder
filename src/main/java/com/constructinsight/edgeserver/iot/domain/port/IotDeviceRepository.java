package com.constructinsight.edgeserver.iot.domain.port;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;

import java.util.List;
import java.util.Optional;

/**
 * Domain Port: Repository interface for IoT Device persistence
 * Following Hexagonal Architecture principles
 */
public interface IotDeviceRepository {

    /**
     * Find device by serial number
     */
    Optional<IotDevice> findBySerialNumber(String serialNumber);

    /**
     * Find all devices owned by a specific user (Privacy Filter)
     */
    List<IotDevice> findAllByOwnerId(String ownerId);

    /**
     * Save or update a device
     */
    IotDevice save(IotDevice device);

    /**
     * Save multiple devices (bulk operation)
     */
    List<IotDevice> saveAll(List<IotDevice> devices);

    /**
     * Find all devices (admin only)
     */
    List<IotDevice> findAll();

    /**
     * Delete a device
     */
    void delete(IotDevice device);

    /**
     * Delete a device by serial number
     */
    void deleteBySerialNumber(String serialNumber);

    /**
     * Check if device exists by serial number
     */
    boolean existsBySerialNumber(String serialNumber);
}
