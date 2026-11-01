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
public class RentalNotificationListener {
    private final CoreServiceClient coreServiceClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(RentalNotificationEvent event) {
        try {
            coreServiceClient.sendNotification(
                    event.getNotification(),
                    event.getAccessToken());
        } catch (AppException exception) {
            log.warn(
                    "Event {} committed, but notification {} could not be delivered: {}",
                    event.getNotification().getReferenceId(),
                    event.getNotification().getEvent(),
                    exception.getMessage());
        }
    }
}
