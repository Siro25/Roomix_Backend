package com.team4.rental.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "contact_cards")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "rental_request_id", nullable = false, unique = true)
    UUID rentalRequestId;

    @Column(name = "landlord_id", nullable = false)
    UUID landlordId;

    @Column(name = "landlord_name", nullable = false, length = 100)
    String landlordName;

    @Column(name = "landlord_phone", nullable = false, length = 20)
    String landlordPhone;

    @Column(name = "landlord_zalo", nullable = false, length = 100)
    String landlordZalo;

    @Column(name = "landlord_avatar_url", length = 500)
    String landlordAvatarUrl;

    @Column(length = 1000)
    String note;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    Instant issuedAt;
}
