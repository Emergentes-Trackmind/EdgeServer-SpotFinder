package com.constructinsight.edgeserver.iot.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

/**
 * Domain Entity: IoT Device
 * Represents a physical IoT device with ownership and privacy controls
 */
@Entity
@Table(name = "iot_devices",
       indexes = {
           @Index(name = "idx_serial_number", columnList = "serialNumber", unique = true),
           @Index(name = "idx_owner_id", columnList = "ownerId")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Serial number is required")
    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @NotBlank(message = "Model is required")
    @Column(nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeviceStatus status;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer battery;

    @Column(nullable = false)
    private Instant lastCheckIn;

    // Privacy Fields - Nullable for unbound devices
    @Column(length = 100)
    private String ownerId;

    @Column(length = 100)
    private String parkingId;

    @Column(length = 100)
    private String parkingSpotId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Business Logic: Bind device to a user
     * This establishes ownership of the device
     */
    public void bind(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.ownerId = userId;
        this.updatedAt = Instant.now();
    }

    /**
     * Business Logic: Unbind device from current owner
     * This removes all privacy-related associations, making the device "free"
     */
    public void unbind() {
        this.ownerId = null;
        this.parkingId = null;
        this.parkingSpotId = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if device is bound to a specific user
     */
    public boolean isBoundToUser(String userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    /**
     * Check if device is currently bound to any user
     */
    public boolean isBound() {
        return this.ownerId != null;
    }
}

