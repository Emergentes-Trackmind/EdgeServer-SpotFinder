package com.constructinsight.edgeserver.iot.infrastructure.web.dto;

import com.constructinsight.edgeserver.iot.domain.model.DeviceStatus;
import com.constructinsight.edgeserver.iot.domain.model.DeviceType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for creating/updating devices (bulk loading)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotDeviceRequestDto {

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Device type is required")
    private DeviceType type;

    @NotNull(message = "Device status is required")
    private DeviceStatus status;

    @Min(0)
    @Max(100)
    @NotNull(message = "Battery level is required")
    private Integer battery;

    private Instant lastCheckIn;
}

