package com.constructinsight.edgeserver.iot.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI Statistics for devices owned by a specific user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceKpiDto {
    private long totalDevices;
    private long onlineDevices;
    private long offlineDevices;
    private double averageBattery;
    private long lowBatteryDevices; // Battery < 20%
}

