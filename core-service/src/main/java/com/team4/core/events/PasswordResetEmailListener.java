package com.team4.core.events;

import com.team4.core.config.PasswordResetProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailListener {
    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetRequestedEvent event) {
        if (properties.getMailFrom() == null || properties.getMailFrom().isBlank()) {
            log.warn("Password reset email was not sent because MAIL_FROM is not configured");
            return;
        }
        try {
            String separator = properties.getUrl().contains("?") ? "&" : "?";
            String resetUrl = properties.getUrl() + separator + "token="
                    + URLEncoder.encode(event.token(), StandardCharsets.UTF_8);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMailFrom());
            message.setTo(event.email());
            message.setSubject("Roomix - Đặt lại mật khẩu");
            message.setText("Chào " + event.fullName() + ",\n\n"
                    + "Bạn đã yêu cầu đặt lại mật khẩu. Mã có hiệu lực trong "
                    + properties.getExpirationMinutes() + " phút.\n"
                    + "Mở liên kết sau để đặt mật khẩu mới:\n" + resetUrl + "\n\n"
                    + "Nếu không phải bạn yêu cầu, hãy bỏ qua email này.");
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Unable to send password reset email", exception);
        }
    }
}
