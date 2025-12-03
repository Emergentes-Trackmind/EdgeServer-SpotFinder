package com.constructinsight.edgeserver.iot.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para sincronizar telemetría de ocupación con el Backend Principal
 * Endpoint destino: POST /api/spots/sync-telemetry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotTelemetrySyncDto {

    private String serialNumber;
    private Boolean occupied;
}

