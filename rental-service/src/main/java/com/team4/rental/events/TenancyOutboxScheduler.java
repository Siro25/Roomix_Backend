package com.team4.rental.events;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenancyOutboxScheduler {
    private final TenancyOutboxPublisher publisher;

    @Scheduled(fixedDelayString = "${roomix.outbox.poll-delay-ms:500}")
    public void publishPending() {
        for (int i = 0; i < 50 && publisher.publishNext(); i++) {
            // Mỗi lệnh gọi đều có giao dịch riêng; không có dữ liệu nào được gửi tới Kafka
            // trước khi thực hiện commit.
        }
    }
}
