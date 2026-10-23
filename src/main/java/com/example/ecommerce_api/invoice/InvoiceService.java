package com.example.ecommerce_api.invoice;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.LayoutProcessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders templates/invoice.html with Thymeleaf and turns it into a PDF. Only paid orders
 * (PAID, PROCESSING, SHIPPED, DELIVERED) have an invoice; the order number is the invoice number.
 *
 * Khmer needs text shaping (subscript consonants, vowels drawn before the consonant), which
 * OpenPDF only does through Java's layout engine once LayoutProcessor is enabled. That path has
 * no per-character font fallback, so the template uses Kantumruy Pro, which covers Khmer and Latin.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;
    private final TemplateEngine templateEngine;
    private final SettingsService settingsService;

    private static final List<String> FONTS = List.of("fonts/KantumruyPro-Regular.ttf", "fonts/KantumruyPro-Bold.ttf");

    /** Fonts copied out of the jar, since OpenPDF loads fonts from file paths. */
    private final List<String> fontPaths = new ArrayList<>();

    @PostConstruct
    void init() throws IOException {
        LayoutProcessor.enable();
        Path dir = Files.createTempDirectory("invoice-fonts");
        dir.toFile().deleteOnExit();
        for (String font : FONTS) {
            Path target = dir.resolve(Path.of(font).getFileName().toString());
            try (InputStream in = new ClassPathResource(font).getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            target.toFile().deleteOnExit();
            fontPaths.add(target.toString());
        }
    }

    @Transactional(readOnly = true)
    public byte[] getMyInvoice(String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> orderNotFound(orderNumber));
        return render(order);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoice(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> orderNotFound(orderNumber));
        return render(order);
    }

    private byte[] render(Order order) {
        if (!OrderStatus.revenueStatuses().contains(order.getStatus())) {
            throw new BusinessException("Order " + order.getOrderNumber() + " is " + order.getStatus()
                    + "; an invoice is only available once it is paid");
        }

        Context ctx = new Context();
        ctx.setVariable("order", order);
        ctx.setVariable("items", order.getOrderItems());
        ctx.setVariable("coupons", order.getOrderCoupons());
        ctx.setVariable("customer", order.getUser());
        // Seller details, currency and dates come from the dashboard settings
        SettingsResponse settings = settingsService.get();
        ctx.setVariable("orderDate", settingsService.toStoreTime(order.getOrderDate()));
        ctx.setVariable("issuedOn", LocalDate.now(settingsService.zone()));
        ctx.setVariable("companyName", settings.getStore().getName());
        ctx.setVariable("companyAddress", settings.getStore().getAddress());
        ctx.setVariable("companyEmail", settings.getStore().getEmail());
        ctx.setVariable("companyPhone", settings.getStore().getPhone());
        ctx.setVariable("currency", settings.getLocalization().getCurrency());
        String html = templateEngine.process("invoice", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // ITextRenderer is not thread-safe, so build one per invoice
            ITextRenderer renderer = new ITextRenderer();
            for (String path : fontPaths) {
                renderer.getFontResolver().addFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate invoice for order " + order.getOrderNumber(), e);
        }
    }

    private static ResourceNotFoundException orderNotFound(String orderNumber) {
        return new ResourceNotFoundException("Order not found: " + orderNumber);
    }
}
