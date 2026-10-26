package com.example.core_service.notifications;

import java.io.IOException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings("unchecked")
class TelegramTextSenderTests {
    private static final TelegramSettings ENABLED = new TelegramSettings("fake-test-token", "123");

    @SuppressWarnings("unchecked")
    private HttpClient transport(int status, String body) throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return client;
    }

    @Test void sendsPlainTextOnlyAndChecksTelegramOk() throws Exception {
        HttpClient client = transport(200, "{\"ok\":true,\"result\":{\"message_id\":1}}");
        var sender = new TelegramTextSender(ENABLED, JsonMapper.builder().build(), client);
        assertThat(sender.send("Roomix | Test").sent()).isTrue();
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri().toString()).isEqualTo("https://api.telegram.org/botfake-test-token/sendMessage");
        assertThat(request.getValue().method()).isEqualTo("POST");
        var body = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        request.getValue().bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { body.onSubscribe(subscription); }
            public void onNext(ByteBuffer buffer) { body.onNext(List.of(buffer)); }
            public void onError(Throwable error) { body.onError(error); }
            public void onComplete() { body.onComplete(); }
        });
        Map<?, ?> parsed = JsonMapper.builder().build().readValue(body.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS), Map.class);
        assertThat(parsed.keySet().stream().map(Object::toString).toList()).containsExactlyInAnyOrder("chat_id", "text");
        assertThat(parsed.get("text")).isEqualTo("Roomix | Test");
    }

    @Test void malformedJsonFalseOkAndRedirectAreNotSuccess() throws Exception {
        for (String payload : new String[] {"<html>error</html>", "{\"ok\":false}", "{\"ok\":\"true\"}", "null", "[]"}) {
            var sender = new TelegramTextSender(ENABLED, JsonMapper.builder().build(), transport(200, payload));
            assertThat(sender.send("test").sent()).isFalse();
        }
        assertThat(new TelegramTextSender(ENABLED, JsonMapper.builder().build(), transport(302, "{\"ok\":true}"))
                .send("test").sent()).isFalse();
    }

    @Test void rateLimitParsesRetryAfter() throws Exception {
        var sender = new TelegramTextSender(ENABLED, JsonMapper.builder().build(),
                transport(429, "{\"ok\":false,\"parameters\":{\"retry_after\":30}}"));
        assertThat(sender.send("test")).isEqualTo(new TelegramTextSender.Result(false, 30));
    }

    @Test void disabledConfigurationDoesNotCallHttp() {
        HttpClient client = mock(HttpClient.class);
        var sender = new TelegramTextSender(new TelegramSettings("", ""), JsonMapper.builder().build(), client);
        assertThat(sender.send("test").sent()).isFalse();
        verifyNoInteractions(client);
    }

    @Test void networkFailureDoesNotExposeExceptionOrToken() throws Exception {
        HttpClient client = transport(200, "{}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("private-bot-token"));
        var sender = new TelegramTextSender(ENABLED, JsonMapper.builder().build(), client);
        assertThat(sender.send("test")).isEqualTo(new TelegramTextSender.Result(false, 0));
    }

    @Test void partialConfigurationIsRejectedWithoutPrintingSecret() {
        assertThatThrownBy(() -> new TelegramSettings("private-token", ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("private-token");
    }
}
