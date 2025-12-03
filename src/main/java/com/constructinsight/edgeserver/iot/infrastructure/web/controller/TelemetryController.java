package com.constructinsight.edgeserver.iot.infrastructure.web.controller;

import com.constructinsight.edgeserver.iot.domain.model.DeviceStatus;
import com.constructinsight.edgeserver.iot.domain.model.DeviceSyncStatus;
import com.constructinsight.edgeserver.iot.domain.model.DeviceType;
import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import com.constructinsight.edgeserver.iot.integration.service.BackendIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

/**
 * Controller para ingesta de telemetría de dispositivos IoT
 * Endpoint público usado por sensores para enviar actualizaciones periódicas
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IoT Telemetry", description = "Ingesta de telemetría de sensores en tiempo real")
public class TelemetryController {

    private final IotDeviceRepository repo;
    private final BackendIntegrationService backendIntegrationService;

    /**
     * POST /api/iot/telemetry - Recibir telemetría de dispositivos

     * Actualiza el estado de un dispositivo existente o lo crea si no existe.
     * Usado por los scripts de simulación para enviar datos periódicos.
     */
    @Operation(
            summary = "Enviar telemetría del sensor",
            description = "Permite que un sensor envíe datos de telemetría (batería, estado, ocupación). " +
                          "Si el dispositivo no existe, se crea automáticamente como libre (sin propietario). " +
                          "Este endpoint debe ser llamado periódicamente por el simulador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Telemetría aceptada y procesada"),
            @ApiResponse(responseCode = "400", description = "Datos de telemetría inválidos", content = @Content)
    })
    @PostMapping("/telemetry")
    public ResponseEntity<Void> ingest(@RequestBody DeviceTelemetryReport telemetry) {
        log.info("📊 [Telemetry] Recibiendo telemetría de: {} (battery: {}%, status: {}, occupied: {})",
                telemetry.serialNumber(),
                telemetry.battery(),
                telemetry.status(),
                telemetry.occupied());

        // Buscar dispositivo por serialNumber
        Optional<IotDevice> opt = repo.findBySerialNumber(telemetry.serialNumber());

        IotDevice dev = opt.orElseGet(() -> {
            // Si no existe, auto-crear como dispositivo descubierto
            log.info("🆕 [Telemetry] Auto-creando dispositivo descubierto: {}", telemetry.serialNumber());

            IotDevice d = new IotDevice();
            d.setSerialNumber(telemetry.serialNumber());
            d.setModel("Auto-Discovered");
            d.setType(DeviceType.SENSOR);
            d.setOwnerId(null);  // 🔑 Sin propietario, libre para ser reclamado
            d.setStatus(DeviceStatus.ONLINE);
            d.setBattery(telemetry.battery() != null ? telemetry.battery() : 100);
            d.setLastCheckIn(Instant.now());
            d.setSyncStatus(DeviceSyncStatus.DISCONNECTED);  // Inicialmente desconectado del backend

            return repo.save(d);
        });

        // Log del estado actual antes de actualizar
        log.info("🔋 [Telemetry] Estado ANTES - Device: {} | Battery actual: {}% | Nueva battery: {}%",
                dev.getSerialNumber(), dev.getBattery(), telemetry.battery());

        // Actualizar campos con datos de telemetría

        // SIEMPRE actualizar batería si viene en la telemetría
        if (telemetry.battery() != null) {
            Integer oldBattery = dev.getBattery();
            dev.setBattery(telemetry.battery());
            log.info("🔋 [Telemetry] Battery actualizada: {} | {}% → {}%",
                    dev.getSerialNumber(), oldBattery, telemetry.battery());
        }

        if (telemetry.status() != null) {
            DeviceStatus newStatus = mapStatus(telemetry.status());
            if (!newStatus.equals(dev.getStatus())) {
                DeviceStatus oldStatus = dev.getStatus();
                dev.setStatus(newStatus);
                log.info("📱 [Telemetry] Status actualizado: {} | {} → {}",
                        dev.getSerialNumber(), oldStatus, newStatus);
            }
        }

        // Actualizar timestamp de última comunicación
        dev.setLastCheckIn(
                telemetry.checkedAt() != null ? telemetry.checkedAt() : Instant.now()
        );

        // Sincronizar estado de ocupación con el Backend Principal
        if (telemetry.occupied() != null) {
            log.info("🅿️ [Telemetry] Sincronizando ocupación con Backend Principal: {} - Occuped: {}",
                    telemetry.serialNumber(), telemetry.occupied());
            backendIntegrationService.notifyBackendOfTelemetry(telemetry.serialNumber(), telemetry.occupied());
        }

        // Persistir cambios SIEMPRE (para actualizar lastCheckIn)
        IotDevice savedDevice = repo.save(dev);

        log.info("✅ [Telemetry] Dispositivo guardado: {} | Battery final: {}% | Status: {} | LastCheckIn: {}",
                savedDevice.getSerialNumber(),
                savedDevice.getBattery(),
                savedDevice.getStatus(),
                savedDevice.getLastCheckIn());

        return ResponseEntity.accepted().build();
    }

    /**
     * Mapea string a DeviceStatus enum
     */
    private DeviceStatus mapStatus(String s) {
        if (s == null) return DeviceStatus.ONLINE;
        return switch (s.toLowerCase()) {
            case "online" -> DeviceStatus.ONLINE;
            case "offline" -> DeviceStatus.OFFLINE;
            case "maintenance" -> DeviceStatus.MAINTENANCE;
            case "error" -> DeviceStatus.ERROR;
            default -> DeviceStatus.ONLINE;
        };
    }

    /**
     * DTO: Reporte de telemetría del dispositivo
     */
    public record DeviceTelemetryReport(
            String serialNumber,
            String status,
            Integer battery,
            Instant checkedAt,
            Boolean occupied,
            HealthMonitor healthMonitor
    ) {}

    /**
     * DTO: Métricas de salud del dispositivo (opcional)
     */
    public record HealthMonitor(
            Integer failuresSinceStartup,
            Integer failuresSinceLastCheckup,
            Integer requestsSinceLastCheckup,
            Integer requestsSinceStartup,
            Double failingRate
    ) {}
}
