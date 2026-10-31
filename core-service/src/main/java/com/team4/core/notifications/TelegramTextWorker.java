package com.team4.core.notifications;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramTextWorker {
    private final TelegramSettings settings;
    private final AuthTelegramRepository queue;
    private final TelegramTextSender sender;
    public TelegramTextWorker(TelegramSettings settings, AuthTelegramRepository queue, TelegramTextSender sender) {
        this.settings = settings; this.queue = queue; this.sender = sender;
    }

    @Transactional
    public boolean deliverNext() {
        if (!settings.enabled()) return false;
        Instant now = Instant.now();
        queue.discardExpired(now);
        var selected = queue.lockNext(now);
        if (selected.isEmpty()) return false;
        var message = selected.get();
        TelegramTextSender.Result result;
        try { result = sender.send(message.text()); }
        catch (RuntimeException exception) { result = new TelegramTextSender.Result(false, 0); }
        queue.finish(message, result, Instant.now());
        return true;
    }
}
