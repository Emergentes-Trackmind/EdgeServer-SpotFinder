package com.constructinsight.edgeserver.iot.integration.service;

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

/**
 * Servicio de integración con el Backend Principal
 * Responsable de sincronizar datos de telemetría en tiempo real
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackendIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${backend.main.url:http://localhost:8081}")
    private String backendMainUrl;

    /**
     * Notifica al Backend Principal sobre nueva telemetría de ocupación
     *
     * @param serialNumber Número de serie del dispositivo/sensor
     * @param occupied Estado de ocupación reportado por el sensor
     */
    public void notifyBackendOfTelemetry(String serialNumber, Boolean occupied) {
        if (occupied == null) {
            log.debug("🔄 [Integration] Telemetría sin dato de ocupación, omitiendo sincronización para: {}", serialNumber);
            return;
        }

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
            } else {
                log.warn("⚠️ [Integration] Sincronización con respuesta no esperada: {} | Status: {}",
                        serialNumber, response.getStatusCode());
            }

        } catch (Exception e) {
            // CRÍTICO: No fallar la ingesta si el Backend no responde
            log.error("❌ [Integration] Error al sincronizar con Backend Principal (sensor: {}): {} - {}",
                    serialNumber, e.getClass().getSimpleName(), e.getMessage());
            log.debug("Stack trace completo:", e);
        }
    }
}

