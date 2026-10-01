package com.team4.core.entities;

import com.team4.core.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(
        name = "uk_rooms_floor_number", columnNames = {"floor_id", "room_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false, updatable = false)
    private Floor floor;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area;
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;
    @Column(name = "max_tenants", nullable = false)
    private int maxTenants;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;
    @Column(name = "has_private_bathroom", nullable = false)
    private boolean hasPrivateBathroom;
    @Column(name = "has_air_conditioner", nullable = false)
    private boolean hasAirConditioner;
    @Column(name = "has_water_heater", nullable = false)
    private boolean hasWaterHeater;
    @Column(name = "has_balcony", nullable = false)
    private boolean hasBalcony;
    @Column(name = "amenities_description", columnDefinition = "text")
    private String amenitiesDescription;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Room(Floor floor) {
        this.floor = floor;
    }

    public void updateDetails(String roomNumber, BigDecimal area, BigDecimal basePrice, int maxTenants,
            boolean hasPrivateBathroom, boolean hasAirConditioner, boolean hasWaterHeater,
            boolean hasBalcony, String amenitiesDescription) {
        this.roomNumber = roomNumber;
        this.area = area;
        this.basePrice = basePrice;
        this.maxTenants = maxTenants;
        this.hasPrivateBathroom = hasPrivateBathroom;
        this.hasAirConditioner = hasAirConditioner;
        this.hasWaterHeater = hasWaterHeater;
        this.hasBalcony = hasBalcony;
        this.amenitiesDescription = amenitiesDescription;
    }
}
