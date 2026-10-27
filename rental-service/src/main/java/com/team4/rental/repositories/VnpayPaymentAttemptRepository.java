package com.team4.rental.repositories;

import com.team4.rental.entities.VnpayPaymentAttempt;
import com.team4.rental.enums.VnpayPaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface VnpayPaymentAttemptRepository extends JpaRepository<VnpayPaymentAttempt, UUID> {
    Optional<VnpayPaymentAttempt> findByInvoiceIdAndStatus(UUID invoiceId, VnpayPaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VnpayPaymentAttempt p where p.txnRef = :txnRef")
    Optional<VnpayPaymentAttempt> lockByTxnRef(String txnRef);

    Optional<VnpayPaymentAttempt> findByTxnRef(String txnRef);
}
