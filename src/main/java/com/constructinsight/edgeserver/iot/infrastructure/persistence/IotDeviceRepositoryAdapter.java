package com.constructinsight.edgeserver.iot.infrastructure.persistence;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter: Bridges Spring Data JPA with Domain Port
 * This adapter makes the infrastructure implementation conform to the domain interface
 */
@Component
@RequiredArgsConstructor
public class IotDeviceRepositoryAdapter implements IotDeviceRepository {

    private final JpaIotDeviceRepository jpaRepository;

    @Override
    public Optional<IotDevice> findBySerialNumber(String serialNumber) {
        return jpaRepository.findBySerialNumber(serialNumber);
    }

    @Override
    public List<IotDevice> findAllByOwnerId(String ownerId) {
        return jpaRepository.findAllByOwnerId(ownerId);
    }

    @Override
    public IotDevice save(IotDevice device) {
        return jpaRepository.save(device);
    }

    @Override
    public List<IotDevice> saveAll(List<IotDevice> devices) {
        return jpaRepository.saveAll(devices);
    }

    @Override
    public List<IotDevice> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void delete(IotDevice device) {
        jpaRepository.delete(device);
    }

    @Override
    public void deleteBySerialNumber(String serialNumber) {
        jpaRepository.deleteBySerialNumber(serialNumber);
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return jpaRepository.existsBySerialNumber(serialNumber);
    }
}
