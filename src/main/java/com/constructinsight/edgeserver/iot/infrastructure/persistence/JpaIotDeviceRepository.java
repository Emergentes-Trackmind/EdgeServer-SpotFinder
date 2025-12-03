package com.constructinsight.edgeserver.iot.infrastructure.persistence;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository (Infrastructure Layer)
 * Implements the domain port interface
 */
@Repository
public interface JpaIotDeviceRepository extends JpaRepository<IotDevice, Long> {

    Optional<IotDevice> findBySerialNumber(String serialNumber);

    List<IotDevice> findAllByOwnerId(String ownerId);

    boolean existsBySerialNumber(String serialNumber);

    void deleteBySerialNumber(String serialNumber);
}
