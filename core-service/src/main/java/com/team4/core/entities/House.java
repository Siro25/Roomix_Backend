package com.team4.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "houses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false, updatable = false)
    private User landlord;

    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "address_street", nullable = false, length = 255)
    private String addressStreet;
    @Column(nullable = false, length = 100)
    private String ward;
    @Column(length = 100)
    private String district;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "total_floors", nullable = false)
    private int totalFloors;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public House(User landlord) {
        this.landlord = landlord;
    }

    public void synchronizeFloorCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Floor count cannot be negative");
        }
        this.totalFloors = count;
    }

    public void updateDetails(String name, String addressStreet, String ward, String district,
            String city, BigDecimal latitude, BigDecimal longitude, String description) {
        this.name = name;
        this.addressStreet = addressStreet;
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }
}
