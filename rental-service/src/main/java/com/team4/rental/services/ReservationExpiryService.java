package com.team4.rental.services;

import com.team4.rental.enums.TenancyStatus;
import com.team4.rental.repositories.TenancyRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationExpiryService {
    private final TenancyRepository tenancies;
    private final Clock clock;

    @Transactional
    public int expireRoom(UUID roomId) {
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        return tenancies.expirePendingForRoom(roomId, clock.instant(),
                TenancyStatus.PENDING, TenancyStatus.CANCELLED, "Quá hạn xác nhận tiền cọc");
    }
}
