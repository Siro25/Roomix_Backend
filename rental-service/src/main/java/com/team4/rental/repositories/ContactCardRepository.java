package com.team4.rental.repositories;

import com.team4.rental.entities.ContactCard;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactCardRepository extends JpaRepository<ContactCard, UUID> {
    Optional<ContactCard> findByRentalRequestId(UUID rentalRequestId);
}
