package com.constructinsight.edgeserver.iot.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for binding operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindDeviceRequestDto {

    @NotBlank(message = "User ID is required")
    private String userId;
}

