package com.example.ecommerce_api.notification.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Telegram Bot API calls against a mock server; nothing leaves the machine. */
class TelegramNotificationStrategyTest {

    private static final String TOKEN = "123456:SECRET-bot-token";
    private static final String SEND_URL = "https://api.telegram.test/bot" + TOKEN + "/sendMessage";

    private MockRestServiceServer server;
    private TelegramNotificationStrategy telegram;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.test");
        server = MockRestServiceServer.bindTo(builder).build();
        telegram = new TelegramNotificationStrategy(TOKEN, builder);
    }

    @Test
    void postsTheMessageToTheChat() {
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("-1001234567890"))
                .andExpect(jsonPath("$.text").value("🛒 New order ORD-1\n\nTotal: 10.00 USD"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        telegram.send("-1001234567890", new NotificationMessage("🛒 New order ORD-1", "Total: 10.00 USD"));

        server.verify();
    }

    @Test
    void telegramErrorIsReportedWithItsReasonButWithoutTheToken() {
        server.expect(requestTo(SEND_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> telegram.send("-100999", new NotificationMessage("t", "b")))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("chat not found")
                .hasNoCause()
                .message().doesNotContain(TOKEN);
    }

    @Test
    void networkFailureDoesNotLeakTheTokenFromTheRequestUrl() {
        server.expect(requestTo(SEND_URL)).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> telegram.send("-100999", new NotificationMessage("t", "b")))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("could not be reached")
                .hasNoCause()
                .message().doesNotContain(TOKEN);
    }

    @Test
    void withoutATokenNothingIsSent() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.test");
        MockRestServiceServer noCalls = MockRestServiceServer.bindTo(builder).build();
        TelegramNotificationStrategy unconfigured = new TelegramNotificationStrategy("  ", builder);

        assertThat(unconfigured.isConfigured()).isFalse();
        assertThatThrownBy(() -> unconfigured.send("-100999", new NotificationMessage("t", "b")))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("TELEGRAM_BOT_TOKEN");
        noCalls.verify();   // no request was made
    }

    @Test
    void longMessagesAreCutToTelegramsLimit() {
        String text = TelegramNotificationStrategy.text(new NotificationMessage("title", "x".repeat(5000)));

        assertThat(text).hasSize(TelegramNotificationStrategy.MAX_TEXT_LENGTH).endsWith("…");
    }
}
