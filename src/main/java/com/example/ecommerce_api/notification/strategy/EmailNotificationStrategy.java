package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

import java.nio.charset.StandardCharsets;

/**
 * HTML email over SMTP, rendered from a Thymeleaf template in resources/templates/email/, with the
 * plain-text body as alternative for clients that don't show HTML.
 *
 * Spring Boot only creates the JavaMailSender when spring.mail.host (SMTP_HOST) is set, so without
 * it this channel reports itself as not configured instead of stopping the app from starting.
 */
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    static final String DEFAULT_TEMPLATE = "email/notification";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final TemplateEngine templateEngine;
    private final String from;

    public EmailNotificationStrategy(ObjectProvider<JavaMailSender> mailSender,
                                     TemplateEngine templateEngine,
                                     @Value("${app.notifications.email-from:}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from == null ? "" : from.trim();
    }

    @Override
    public NotificationType type() {
        return NotificationType.EMAIL;
    }

    @Override
    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && StringUtils.hasText(from);
    }

    @Override
    public void send(String target, NotificationMessage message) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(from)) {
            throw new NotificationException("Email is not configured: set SMTP_HOST and EMAIL_FROM");
        }

        String html = render(message);
        try {
            MimeMessage mail = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(target);
            helper.setSubject(message.title());
            helper.setText(message.body(), html);
            sender.send(mail);
        } catch (MessagingException e) {
            throw new NotificationException("Email could not be built: " + e.getMessage());
        } catch (MailException e) {
            throw new NotificationException("Email could not be sent: " + e.getMostSpecificCause().getMessage());
        }
    }

    String render(NotificationMessage message) {
        String template = StringUtils.hasText(message.emailTemplate()) ? message.emailTemplate() : DEFAULT_TEMPLATE;
        Context context = new Context();
        context.setVariables(message.model());
        context.setVariable("title", message.title());
        context.setVariable("body", message.body());
        try {
            return templateEngine.process(template, context);
        } catch (TemplateEngineException e) {
            throw new NotificationException("Email template " + template + " could not be rendered: " + e.getMessage());
        }
    }
}
