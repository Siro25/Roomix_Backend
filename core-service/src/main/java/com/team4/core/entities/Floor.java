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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "floors", uniqueConstraints = @UniqueConstraint(
        name = "uk_floors_house_number", columnNames = {"house_id", "floor_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false, updatable = false)
    private House house;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "floor", fetch = FetchType.LAZY)
    @OrderBy("roomNumber ASC")
    private List<Room> rooms = new ArrayList<>();

    public Floor(House house, int floorNumber, String name, String description) {
        this.house = house;
        updateDetails(floorNumber, name, description);
    }

    public void updateDetails(int floorNumber, String name, String description) {
        this.floorNumber = floorNumber;
        this.name = name;
        this.description = description;
    }
}
