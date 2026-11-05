package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.notification.service.NotificationMessages;
import com.example.ecommerce_api.order.dto.OrderCustomerResponse;
import com.example.ecommerce_api.order.dto.OrderItemResponse;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.order.dto.ShippingAddressResponse;
import com.example.ecommerce_api.product.dto.response.BrandResponse;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;
import com.example.ecommerce_api.product.dto.response.ProductVariantResponse;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HTML email from the real Thymeleaf templates in resources/templates/email, with a mocked
 * JavaMailSender; no SMTP server involved.
 */
class EmailNotificationStrategyTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SpringTemplateEngine templateEngine = templateEngine();
    private NotificationMessages messages;

    /** Same template location and mode as Spring Boot's Thymeleaf auto-configuration. */
    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    private EmailNotificationStrategy email(JavaMailSender sender, String from) {
        return new EmailNotificationStrategy(provider(sender), templateEngine, from);
    }

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.get()).thenReturn(new SettingsResponse(
                new SettingsResponse.Store("Ratana Shop", "hello@ratana.shop", "012 345 678", null, null),
                null,
                new SettingsResponse.Inventory(10),
                new SettingsResponse.Notifications(true),
                new SettingsResponse.Localization("USD", "Asia/Phnom_Penh"),
                null, null, null));
        messages = new NotificationMessages(settingsService);
    }

    // ----- sending ----------------------------------------------------------------------------

    @Test
    void sendsAnHtmlEmailWithSubjectAndAddresses() throws Exception {
        email(mailSender, "shop@example.com")
                .send("buyer@example.com", messages.lowStock("Basic Tee", "TEE-M-BLK", 4, 10));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("⚠️ Low stock: Basic Tee");
        assertThat(((InternetAddress) sent.getValue().getFrom()[0]).getAddress()).isEqualTo("shop@example.com");
        assertThat(((InternetAddress) sent.getValue().getAllRecipients()[0]).getAddress()).isEqualTo("buyer@example.com");
    }

    @Test
    void smtpFailureBecomesANotificationException() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> email(mailSender, "shop@example.com")
                .send("buyer@example.com", new NotificationMessage("t", "b")))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    void withoutSmtpHostOrSenderItIsNotConfigured() {
        EmailNotificationStrategy noSmtp = email(null, "shop@example.com");
        EmailNotificationStrategy noFrom = email(mailSender, "");

        assertThat(noSmtp.isConfigured()).isFalse();
        assertThat(noFrom.isConfigured()).isFalse();
        assertThatThrownBy(() -> noSmtp.send("buyer@example.com", new NotificationMessage("t", "b")))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("SMTP_HOST");
    }

    @Test
    void missingTemplateBecomesANotificationException() {
        NotificationMessage message = new NotificationMessage("t", "b", "email/does-not-exist", null);

        assertThatThrownBy(() -> email(mailSender, "shop@example.com").send("buyer@example.com", message))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("email/does-not-exist");
    }

    // ----- templates --------------------------------------------------------------------------

    @Test
    void newOrderTemplateShowsItemsAndTotal() {
        OrderResponse order = OrderResponse.builder()
                .orderNumber("ORD-20261024-ABC")
                .status(OrderStatus.PENDING)
                .customer(OrderCustomerResponse.builder().name("Dara Sok").email("dara@example.com").build())
                .items(List.of(OrderItemResponse.builder()
                        .productName("Basic Tee").sku("TEE-M-BLK").color("Black").sizeName("M")
                        .quantity(2).lineTotal(new BigDecimal("20.00")).build()))
                .discountAmount(new BigDecimal("2.00"))
                .totalOrderPrice(new BigDecimal("1018.00"))
                .build();

        String html = email(mailSender, "shop@example.com").render(messages.newOrder(order));

        assertThat(html)
                .contains("Ratana Shop")
                .contains("New order ORD-20261024-ABC")
                .contains("Dara Sok")
                .contains("Basic Tee")
                .contains("TEE-M-BLK · Black · M")
                .contains("20.00 USD")
                .contains("-2.00 USD")
                .contains("1,018.00 USD");
    }

    @Test
    void orderConfirmationTemplateIsWrittenForTheCustomer() {
        OrderResponse order = OrderResponse.builder()
                .orderNumber("ORD-20261024-ABC")
                .status(OrderStatus.PENDING)
                .customer(OrderCustomerResponse.builder().name("Dara Sok").email("dara@example.com").build())
                .shippingAddress(ShippingAddressResponse.builder()
                        .recipientName("Dara Sok").phone("012 999 888").street("Street 271")
                        .city("Phnom Penh").zipCode("12000").country("Cambodia").build())
                .items(List.of(OrderItemResponse.builder()
                        .productName("Basic Tee").color("Black").sizeName("M").imageUrl("https://img.example.com/tee.jpg")
                        .quantity(2).lineTotal(new BigDecimal("20.00")).build()))
                .subtotal(new BigDecimal("20.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxPrice(BigDecimal.ZERO)
                .shippingPrice(BigDecimal.ZERO)
                .totalOrderPrice(new BigDecimal("20.00"))
                .build();

        NotificationMessage message = messages.orderConfirmation(order);
        String html = email(mailSender, "shop@example.com").render(message);

        assertThat(message.title()).isEqualTo("Your Ratana Shop order ORD-20261024-ABC is received");
        assertThat(message.body()).startsWith("Hi Dara Sok,").contains("Total: 20.00 USD").contains("Street 271");
        assertThat(html)
                .contains("Thank you for your order!")
                .contains("Hi Dara Sok,")
                .contains("ORD-20261024-ABC")
                .contains("https://img.example.com/tee.jpg")
                .contains("Black · M · Qty 2")
                .contains("Free")                       // no shipping fee
                .contains("Phnom Penh 12000")
                .contains("mailto:hello@ratana.shop")   // customer footer: store contact
                .doesNotContain("admin system")         // not the admin footer
                .doesNotContain("Discount");            // zero discount is hidden
    }

    @Test
    void adminEmailsKeepTheAdminFooter() {
        assertThat(email(mailSender, "shop@example.com").render(messages.lowStock("Basic Tee", "TEE-M-BLK", 4, 10)))
                .contains("admin system")
                .doesNotContain("Questions about your order");
    }

    @Test
    void lowStockTemplateDistinguishesOutOfStock() {
        EmailNotificationStrategy email = email(mailSender, "shop@example.com");

        assertThat(email.render(messages.lowStock("Basic Tee", "TEE-M-BLK", 4, 10)))
                .contains("LOW STOCK").contains("TEE-M-BLK").contains(">4<").doesNotContain("OUT OF STOCK");
        assertThat(email.render(messages.lowStock("Basic Tee", "TEE-M-BLK", 0, 10)))
                .contains("OUT OF STOCK");
    }

    @Test
    void newProductTemplateShowsVariantsAndPrice() {
        BrandResponse brand = BrandResponse.builder().brandName("Uniq").build();
        ProductResponse product = ProductResponse.builder()
                .title("Basic Tee").slug("basic-tee").brand(brand)
                .variants(List.of(
                        ProductVariantResponse.builder().sku("TEE-M-BLK").color("Black")
                                .size(ProductSizeResponse.builder().name("M").build())
                                .price(new BigDecimal("12.00")).priceAfterDiscount(new BigDecimal("9.50"))
                                .availableQuantity(20).build(),
                        ProductVariantResponse.builder().sku("TEE-L-BLK").color("Black")
                                .price(new BigDecimal("12.00")).availableQuantity(5).build()))
                .build();

        String html = email(mailSender, "shop@example.com").render(messages.newProduct(product));

        assertThat(html)
                .contains("New product: Basic Tee")
                .contains("Uniq")
                .contains("9.50 USD")       // cheapest price, after discount
                .contains("Black · M")
                .contains("TEE-L-BLK")
                .contains(">25<");          // total stock
    }

    @Test
    void messageWithoutItsOwnTemplateUsesTheGenericOne() {
        String html = email(mailSender, "shop@example.com")
                .render(new NotificationMessage("✅ Test notification", "\"Sales team\" is set up."));

        assertThat(html).contains("✅ Test notification").contains("&quot;Sales team&quot; is set up.");
    }
}
