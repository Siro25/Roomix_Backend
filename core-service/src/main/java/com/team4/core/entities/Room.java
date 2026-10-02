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
import lombok.experimental.FieldDefaults;

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
    Floor floor;

    @Column(name = "room_number", nullable = false, length = 50)
    String roomNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal area;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    BigDecimal basePrice;

    @Column(name = "max_tenants", nullable = false)
    int maxTenants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RoomStatus status;
}
