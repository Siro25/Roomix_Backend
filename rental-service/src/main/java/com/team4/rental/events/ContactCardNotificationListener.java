package com.team4.rental.events;

import com.team4.rental.exceptions.AppException;
import com.team4.rental.external.CoreServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactCardNotificationListener {
    private final CoreServiceClient coreServiceClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyTenant(ContactCardIssuedEvent event) {
        try {
            coreServiceClient.sendContactCardNotification(
                    event.getTenantId(),
                    event.getRentalRequestId(),
                    event.getAccessToken());
        } catch (AppException exception) {
            log.warn(
                    "Contact card {} was created, but its notification could not be delivered: {}",
                    event.getRentalRequestId(),
                    exception.getMessage());
        }
    }
}
