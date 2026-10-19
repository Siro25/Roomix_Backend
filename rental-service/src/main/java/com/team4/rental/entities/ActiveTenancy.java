package com.team4.rental.entities;

import com.team4.rental.enums.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "active_tenancies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTenancy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID roomId;

    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID tenancyId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant assignedAt;
}
