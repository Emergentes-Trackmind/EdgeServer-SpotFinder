package com.constructinsight.edgeserver.iot.integration.service;

import com.constructinsight.edgeserver.iot.domain.model.DeviceSyncStatus;
import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.domain.port.IotDeviceRepository;
import com.constructinsight.edgeserver.iot.integration.dto.SpotTelemetrySyncDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Servicio de integración con el Backend Principal
 * Responsable de sincronizar datos de telemetría en tiempo real
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackendIntegrationService {

    private final RestTemplate restTemplate;
    private final IotDeviceRepository deviceRepository;

    @Value("${backend.main.url:http://localhost:8081}")
    private String backendMainUrl;

    /**
     * Notifica al Backend Principal sobre nueva telemetría de ocupación
     * y actualiza el estado de sincronización del dispositivo localmente
     *
     * @param serialNumber Número de serie del dispositivo/sensor
     * @param occupied Estado de ocupación reportado por el sensor
     */
    public void notifyBackendOfTelemetry(String serialNumber, Boolean occupied) {
        if (occupied == null) {
            log.debug("🔄 [Integration] Telemetría sin dato de ocupación, omitiendo sincronización para: {}", serialNumber);
            return;
        }

        DeviceSyncStatus newSyncStatus = DeviceSyncStatus.DISCONNECTED; // Por defecto: desconectado

        try {
            String endpoint = backendMainUrl + "/api/spots/sync-telemetry";

            SpotTelemetrySyncDto payload = SpotTelemetrySyncDto.builder()
                    .serialNumber(serialNumber)
                    .occupied(occupied)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SpotTelemetrySyncDto> request = new HttpEntity<>(payload, headers);

            log.info("🔄 [Integration] Sincronizando telemetría con Backend: {} → {} (occupied: {})",
                    serialNumber, endpoint, occupied);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Integration] Sincronización exitosa: {} | Status: {}",
                        serialNumber, response.getStatusCode());
                newSyncStatus = DeviceSyncStatus.CONNECTED; // Backend respondió OK
            } else {
                log.warn("⚠️ [Integration] Sincronización con respuesta no esperada: {} | Status: {}",
                        serialNumber, response.getStatusCode());
                newSyncStatus = DeviceSyncStatus.DISCONNECTED; // Backend respondió con error
            }

        } catch (Exception e) {
            // CRÍTICO: No fallar la ingesta si el Backend no responde
            log.error("❌ [Integration] Error al sincronizar con Backend Principal (sensor: {}): {} - {}",
                    serialNumber, e.getClass().getSimpleName(), e.getMessage());
            log.debug("Stack trace completo:", e);
            newSyncStatus = DeviceSyncStatus.DISCONNECTED; // Error de conexión
        } finally {
            // Actualizar el estado de sincronización en la base de datos local
            updateDeviceSyncStatus(serialNumber, newSyncStatus);
        }
    }

    /**
     * Actualiza el estado de sincronización del dispositivo en la BD local
     *
     * @param serialNumber Número de serie del dispositivo
     * @param newSyncStatus Nuevo estado de sincronización
     */
    private void updateDeviceSyncStatus(String serialNumber, DeviceSyncStatus newSyncStatus) {
        try {
            Optional<IotDevice> deviceOpt = deviceRepository.findBySerialNumber(serialNumber);

            if (deviceOpt.isPresent()) {
                IotDevice device = deviceOpt.get();
                DeviceSyncStatus oldSyncStatus = device.getSyncStatus();

                // Solo actualizar si cambió el estado
                if (oldSyncStatus != newSyncStatus) {
                    device.setSyncStatus(newSyncStatus);
                    deviceRepository.save(device);

                    log.info("🔄 [Integration] Estado de sincronización actualizado: {} | {} → {}",
                            serialNumber, oldSyncStatus, newSyncStatus);
                } else {
                    log.debug("🔄 [Integration] Estado de sincronización sin cambios: {} | Status: {}",
                            serialNumber, newSyncStatus);
                }
            } else {
                log.warn("⚠️ [Integration] No se pudo actualizar syncStatus - Dispositivo no encontrado: {}", serialNumber);
            }
        } catch (Exception e) {
            log.error("❌ [Integration] Error al actualizar syncStatus del dispositivo {}: {}",
                    serialNumber, e.getMessage());
        }
    }
}
