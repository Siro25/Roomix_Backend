package com.team4.rental.repositories;

import com.team4.rental.entities.InvoiceItem;
import com.team4.rental.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    List<InvoiceItem> findByInvoiceIdOrderByItemType(UUID invoiceId);
}
