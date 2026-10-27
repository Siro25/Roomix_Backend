package com.team4.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

@Entity
@Table(name = "houses")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "landlord_id", nullable = false)
    UUID landlordId;

    @Column(nullable = false, length = 150)
    @Setter
    String name;

    @Column(name = "address_street", nullable = false, length = 255)
    @Setter
    String addressStreet;

    @Column(nullable = false, length = 100)
    @Setter
    String ward;

    @Column(nullable = false, length = 100)
    @Setter
    String district;

    @Column(nullable = false, length = 100)
    @Setter
    String city;

    @Column(precision = 10, scale = 7)
    @Setter
    BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    @Setter
    BigDecimal longitude;

    @Column(columnDefinition = "text")
    @Setter
    String description;

    @Column(name = "total_floors", nullable = false)
    @Setter
    int totalFloors;

    @Builder.Default
    @OneToMany(mappedBy = "house", fetch = FetchType.LAZY)
    @OrderBy("floorNumber ASC")
    List<Floor> floors = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
