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
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "rooms")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    @Setter
    Floor floor;

    @Column(name = "room_number", nullable = false, length = 50)
    @Setter
    String roomNumber;

    @Setter
    @Column(precision = 10, scale = 2)
    BigDecimal area;

    @Setter
    @Column(name = "base_price", precision = 15, scale = 2)
    BigDecimal basePrice;

    @Setter
    @Column(name = "max_tenants", nullable = false)
    int maxTenants;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RoomStatus status = RoomStatus.AVAILABLE;

    @Setter
    @Column(name = "has_private_bathroom", nullable = false)
    boolean hasPrivateBathroom;

    @Setter
    @Column(name = "has_air_conditioner", nullable = false)
    boolean hasAirConditioner;

    @Setter
    @Column(name = "has_water_heater", nullable = false)
    boolean hasWaterHeater;

    @Setter
    @Column(name = "has_balcony", nullable = false)
    boolean hasBalcony;

    @Setter
    @Column(name = "amenities_description", columnDefinition = "text")
    String amenitiesDescription;

    @Builder.Default
    @Column(name = "is_rented", nullable = false)
    boolean rented = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
