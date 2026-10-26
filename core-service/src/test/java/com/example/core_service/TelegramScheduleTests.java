package com.example.core_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.core_service.notifications.TelegramTextWorker;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TEST_CORE_DB_URL", matches = ".+")
class TelegramScheduleTests {
    @MockitoBean TelegramTextWorker worker;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        CoreServiceApplicationTests.database(registry);
        registry.add("core.telegram.worker-enabled", () -> "true");
    }

    @Test void enablingScheduleInvokesWorkerWithoutSendingRealTelegram() {
        verify(worker, timeout(6500).atLeastOnce()).deliverNext();
    }
}
