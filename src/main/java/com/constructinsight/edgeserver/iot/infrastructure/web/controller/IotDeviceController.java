package com.constructinsight.edgeserver.iot.infrastructure.web.controller;

import com.constructinsight.edgeserver.iot.application.dto.DeviceKpiDto;
import com.constructinsight.edgeserver.iot.application.service.DeviceManagementService;
import com.constructinsight.edgeserver.iot.application.service.DeviceOwnershipService;
import com.constructinsight.edgeserver.iot.application.service.DeviceQueryService;
import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import com.constructinsight.edgeserver.iot.infrastructure.web.dto.BindDeviceRequestDto;
import com.constructinsight.edgeserver.iot.infrastructure.web.dto.IotDeviceRequestDto;
import com.constructinsight.edgeserver.iot.infrastructure.web.dto.IotDeviceResponseDto;
import com.constructinsight.edgeserver.iot.infrastructure.web.mapper.IotDeviceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller: IoT Device Management
 * Exposes endpoints for device binding, querying, and bulk operations
 */
@RestController
@RequestMapping("/api/iot/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IoT Devices", description = "API para gestión de dispositivos IoT con controles de privacidad")
public class IotDeviceController {

    private final DeviceOwnershipService ownershipService;
    private final DeviceQueryService queryService;
    private final IotDeviceRepository deviceRepository;
    private final IotDeviceMapper deviceMapper;
    private final DeviceManagementService managementService;

    /**
     * GET /api/iot/devices
     * Retrieve all devices owned by the user
     */
    @Operation(
            summary = "Obtener dispositivos del usuario",
            description = "Retorna todos los dispositivos IoT que pertenecen al usuario especificado. " +
                          "Solo se retornan dispositivos donde ownerId coincide con el userId proporcionado (Privacy Filter)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de dispositivos obtenida exitosamente"),
            @ApiResponse(responseCode = "400", description = "userId no proporcionado", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<IotDeviceResponseDto>> getUserDevices(
            @Parameter(description = "ID del usuario (desde header)", example = "alice")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Parameter(description = "ID del usuario (desde query param)", example = "alice")
            @RequestParam(value = "userId", required = false) String paramUserId) {

        // Extract userId from header or query parameter
        String userId = headerUserId != null ? headerUserId : paramUserId;

        if (userId == null || userId.isBlank()) {
            log.warn("Missing userId in request");
            return ResponseEntity.badRequest().build();
        }

        log.info("Fetching devices for user: {}", userId);
        List<IotDevice> devices = queryService.findAllByUser(userId);
        List<IotDeviceResponseDto> response = deviceMapper.toResponseDtoList(devices);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/iot/devices/kpis
     * Get KPI statistics for user's devices
     */
    @Operation(
            summary = "Obtener KPIs de dispositivos",
            description = "Calcula estadísticas (total, online, offline, batería promedio) " +
                          "solo para los dispositivos del usuario especificado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KPIs calculados exitosamente"),
            @ApiResponse(responseCode = "400", description = "userId no proporcionado", content = @Content)
    })
    @GetMapping("/kpis")
    public ResponseEntity<DeviceKpiDto> getDeviceKpis(
            @Parameter(description = "ID del usuario", example = "alice")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Parameter(description = "ID del usuario", example = "alice")
            @RequestParam(value = "userId", required = false) String paramUserId) {

        String userId = headerUserId != null ? headerUserId : paramUserId;

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Fetching KPIs for user: {}", userId);
        DeviceKpiDto kpis = queryService.getKpis(userId);

        return ResponseEntity.ok(kpis);
    }

    /**
     * POST /api/iot/devices/{serialNumber}/bind
     * Bind a device to a user (establish ownership)
     */
    @Operation(
            summary = "Vincular dispositivo a usuario",
            description = "Establece ownership del dispositivo. Solo funciona si el dispositivo está libre (ownerId=null) " +
                          "o ya pertenece al usuario que lo solicita."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo vinculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Dispositivo ya vinculado a otro usuario", content = @Content)
    })
    @PostMapping("/{serialNumber}/bind")
    public ResponseEntity<?> bindDevice(
            @Parameter(description = "Número de serie del dispositivo", example = "SENSOR-001")
            @PathVariable String serialNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de vinculación con userId",
                    required = true
            )
            @Valid @RequestBody BindDeviceRequestDto request) {

        try {
            log.info("Binding request for device {} to user {}", serialNumber, request.getUserId());
            IotDevice boundDevice = ownershipService.bindDevice(serialNumber, request.getUserId());
            IotDeviceResponseDto response = deviceMapper.toResponseDto(boundDevice);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Bind failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Bind failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * DELETE /api/iot/devices/{serialNumber}/bind
     * Unbind a device from its owner (privacy control - only owner can unbind)
     */
    @Operation(
            summary = "Desvincular dispositivo",
            description = "Libera el dispositivo (ownerId=null). CONTROL DE PRIVACIDAD: " +
                          "Solo el propietario actual puede desvincular su dispositivo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo desvinculado exitosamente"),
            @ApiResponse(responseCode = "400", description = "userId no proporcionado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuario no es el propietario (Security)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado", content = @Content)
    })
    @DeleteMapping("/{serialNumber}/bind")
    public ResponseEntity<?> unbindDevice(
            @Parameter(description = "Número de serie del dispositivo", example = "SENSOR-001")
            @PathVariable String serialNumber,
            @Parameter(description = "ID del usuario propietario", example = "alice")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Parameter(description = "ID del usuario propietario", example = "alice")
            @RequestParam(value = "userId", required = false) String paramUserId) {

        String userId = headerUserId != null ? headerUserId : paramUserId;

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("User ID is required"));
        }

        try {
            log.info("Unbind request for device {} from user {}", serialNumber, userId);
            IotDevice unboundDevice = ownershipService.unbindDevice(serialNumber, userId);
            IotDeviceResponseDto response = deviceMapper.toResponseDto(unboundDevice);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Unbind failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (SecurityException e) {
            log.error("Unbind failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/iot/devices/bulk
     * Admin endpoint: Bulk load devices into the database
     */
    @Operation(
            summary = "Carga masiva de dispositivos (Admin)",
            description = "Carga múltiples dispositivos en la base de datos. " +
                          "Los dispositivos se crean con ownerId=null (estado libre) y pueden ser vinculados posteriormente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dispositivos creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos en el request", content = @Content)
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<IotDeviceResponseDto>> bulkCreateDevices(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Lista de dispositivos a crear",
                    required = true
            )
            @Valid @RequestBody List<IotDeviceRequestDto> requests) {

        log.info("Bulk creating {} devices", requests.size());

        List<IotDevice> devices = requests.stream()
                .map(deviceMapper::toEntity)
                .toList();

        List<IotDevice> savedDevices = deviceRepository.saveAll(devices);
        List<IotDeviceResponseDto> response = deviceMapper.toResponseDtoList(savedDevices);

        log.info("Successfully created {} devices", savedDevices.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/iot/devices/{serialNumber}
     * Permanently delete a device from the database by serial number
     */
    @Operation(
            summary = "Eliminar dispositivo permanentemente",
            description = "Elimina de forma permanente un dispositivo IoT de la base de datos usando su número de serie. " +
                          "Esta operación es irreversible y elimina completamente el dispositivo del sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dispositivo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado", content = @Content)
    })
    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<?> deleteDevice(
            @Parameter(description = "Número de serie del dispositivo a eliminar", example = "SENSOR-001")
            @PathVariable String serialNumber) {

        try {
            log.info("Delete request for device: {}", serialNumber);
            managementService.deleteDevice(serialNumber);

            log.info("Device {} successfully deleted", serialNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Error response DTO
     */
    private record ErrorResponse(String message) {}
}
