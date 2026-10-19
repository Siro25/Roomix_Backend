package com.team4.rental.jobs;

import com.team4.rental.enums.TenancyStatus;
import com.team4.rental.repositories.TenancyRepository;
import com.team4.rental.services.ReservationExpiryService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "rental.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {
    private final TenancyRepository tenancies;
    private final ReservationExpiryService expiryService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${rental.jobs.expiry-delay-ms:60000}")
    public void expireReservations() {
        for (var roomId : tenancies.findExpiredRoomIds(TenancyStatus.PENDING, clock.instant(), PageRequest.of(0, 100))) {
            try {
                expiryService.expireRoom(roomId);
            } catch (RuntimeException exception) {
                log.error("Không thể giải phóng giữ chỗ quá hạn của phòng {}", roomId, exception);
            }
        }
    }
}
