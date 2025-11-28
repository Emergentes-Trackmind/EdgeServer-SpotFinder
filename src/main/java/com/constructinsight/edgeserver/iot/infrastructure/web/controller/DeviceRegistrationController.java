package com.constructinsight.edgeserver.iot.infrastructure.web.controller;

import com.constructinsight.edgeserver.iot.domain.model.DeviceStatus;
import com.constructinsight.edgeserver.iot.domain.model.DeviceType;
import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Controller para registro de dispositivos IoT simulados
 * Endpoint público usado por los scripts de simulación para auto-registrarse
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IoT Device Registration", description = "Registro automático de sensores simulados")
public class DeviceRegistrationController {

    private final IotDeviceRepository repo;

    /**
     * POST /api/iot/devices - Registrar un nuevo dispositivo IoT
     *
     * Este endpoint es usado por los scripts de simulación para auto-registrarse
     * al iniciar. Crea dispositivos con ownerId=null (libres, sin propietario).
     */
    @Operation(
            summary = "Registrar sensor simulado",
            description = "Permite que un sensor simulado se auto-registre en el sistema. " +
                          "El dispositivo se crea sin propietario (ownerId=null) y puede ser " +
                          "reclamado posteriormente por un usuario mediante el endpoint de binding."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de registro inválidos", content = @Content)
    })
    @PostMapping("/devices")
    public ResponseEntity<RegisterDeviceResponse> register(@RequestBody RegisterDeviceRequest req) {
        log.info("📡 [Device Registration] Registrando sensor: {}", req.serialNumber());

        // Buscar si ya existe
        Optional<IotDevice> existing = repo.findBySerialNumber(req.serialNumber());

        IotDevice device = existing.orElseGet(() -> {
            log.info("✨ [Device Registration] Creando nuevo dispositivo: {}", req.serialNumber());

            IotDevice d = new IotDevice();
            d.setSerialNumber(req.serialNumber());
            d.setModel(req.model() != null ? req.model() : "Unknown");
            d.setType(mapType(req.type()));
            d.setStatus(mapStatus(req.status()));
            d.setBattery(100);
            d.setOwnerId(null);  // 🔑 CRUCIAL: Sin propietario, dispositivo libre
            d.setParkingId(req.parkingId());
            d.setParkingSpotId(req.parkingSpotId());
            d.setLastCheckIn(Instant.now());

            return repo.save(d);
        });

        log.info("✅ [Device Registration] Dispositivo registrado: {} (ID: {})",
                device.getSerialNumber(), device.getId());

        RegisterDeviceResponse resp = new RegisterDeviceResponse(
                device.getId() != null ? device.getId().toString() : device.getSerialNumber(),
                device.getSerialNumber(),
                ""  // deviceToken no usado por ahora, futuro: generar token JWT
        );

        return ResponseEntity.ok(resp);
    }

    /**
     * Mapea string a DeviceType enum
     */
    private DeviceType mapType(String t) {
        if (t == null) return DeviceType.SENSOR;
        return switch (t.toLowerCase()) {
            case "sensor" -> DeviceType.SENSOR;
            case "camera" -> DeviceType.CAMERA;
            case "barrier", "actuator" -> DeviceType.ACTUATOR;
            case "gateway" -> DeviceType.GATEWAY;
            case "tracker" -> DeviceType.TRACKER;
            default -> DeviceType.SENSOR;
        };
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
     * DTO: Request para registrar dispositivo
     */
    public record RegisterDeviceRequest(
            String serialNumber,
            String model,
            String type,
            String parkingId,
            String parkingSpotId,
            String status
    ) {}

    /**
     * DTO: Response del registro
     */
    public record RegisterDeviceResponse(
            String id,
            String serialNumber,
            String deviceToken
    ) {}
}

