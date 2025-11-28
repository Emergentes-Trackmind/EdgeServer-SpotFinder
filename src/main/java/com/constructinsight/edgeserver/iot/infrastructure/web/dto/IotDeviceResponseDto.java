package com.constructinsight.edgeserver.iot.infrastructure.web.dto;

import com.constructinsight.edgeserver.iot.domain.model.DeviceStatus;
import com.constructinsight.edgeserver.iot.domain.model.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for device information
 * Exposed to REST API clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotDeviceResponseDto {
    private Long id;
    private String serialNumber;
    private String model;
    private DeviceType type;
    private DeviceStatus status;
    private Integer battery;
    private Instant lastCheckIn;
    private String ownerId;
    private String parkingId;
    private String parkingSpotId;
    private Instant createdAt;
    private Instant updatedAt;
}

