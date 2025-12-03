package com.constructinsight.edgeserver.iot.domain.model;

/**
 * Estado de sincronización del dispositivo con el Backend Principal
 */
public enum DeviceSyncStatus {
    /**
     * Dispositivo conectado y sincronizado con el backend
     */
    CONNECTED,

    /**
     * Dispositivo desconectado o sin sincronización con el backend
     */
    DISCONNECTED
}

