// package org.example.reportsystem.controller;
package org.example.reportsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.EmailMessage;
import org.example.reportsystem.dto.SendEmailRequest;
import org.example.reportsystem.repository.UserRepository;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RabbitTemplate rabbit;
    private final UserRepository users;
    private final ConnectionFactory connectionFactory; // для health-пинга

    @Value("${mailer.exchange}") String ex;        // email.exchange
    @Value("${mailer.routing-key}") String rk;     // email.send

    // Простая DTO для ошибки (локально, чтобы не плодить файлы)
    public record ApiError(String code, String message) {
        public static ApiError of(String code, String message) { return new ApiError(code, message); }
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody SendEmailRequest req) {
        List<String> emails = users.findEmailsByIds(req.getUserIds());
        if (emails.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiError.of("NO_RECIPIENTS", "no recipients"));
        }

        // общие переменные для шаблона
        Map<String,Object> baseVars = new HashMap<>();
        if (req.getVars() != null) baseVars.putAll(req.getVars());
        if (req.getUrl() != null && !req.getUrl().isBlank()) baseVars.put("url", req.getUrl());

        try {
            if (!req.isPersonalize()) {
                EmailMessage msg = new EmailMessage(
                        UUID.randomUUID().toString(),
                        emails,
                        req.getSubject(),
                        req.getTemplate(),
                        baseVars,
                        emptyIfNull(req.getCc()),
                        emptyIfNull(req.getBcc())
                );
                rabbit.convertAndSend(ex, rk, msg);
                return ResponseEntity.ok().build();
            }

            // персонально каждому (можно трекать по messageId)
            for (String email : emails) {
                Map<String,Object> vars = new HashMap<>(baseVars);
                vars.putIfAbsent("user", email);
                EmailMessage msg = new EmailMessage(
                        UUID.randomUUID().toString(),
                        List.of(email),
                        req.getSubject(),
                        req.getTemplate(),
                        vars,
                        emptyIfNull(req.getCc()),
                        emptyIfNull(req.getBcc())
                );
                rabbit.convertAndSend(ex, rk, msg);
            }
            return ResponseEntity.ok().build();

        } catch (AmqpConnectException e) {
            // Брокер/коннект недоступны → 503
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "30")
                    .body(ApiError.of("MAIL_BACKEND_UNAVAILABLE",
                            "Сервис рассылки недоступен (нет соединения с брокером). Повторите позже."));
        } catch (AmqpException e) {
            // Любая другая AMQP-проблема → тоже 503, но другим текстом
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "30")
                    .body(ApiError.of("MAIL_PUBLISH_FAILED",
                            "Не удалось поставить сообщение в очередь. Повторите позже."));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.of("INTERNAL_ERROR", "Произошла внутренняя ошибка."));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try (var c = connectionFactory.createConnection()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                    .body(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                    .body(ApiError.of("MAIL_BACKEND_UNAVAILABLE", "Сервис рассылки недоступен"));
        }
    }


    private static List<String> emptyIfNull(List<String> l) {
        return l == null ? List.of() : l;
    }
}
