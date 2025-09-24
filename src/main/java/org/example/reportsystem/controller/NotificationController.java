// package org.example.reportsystem.controller;
package org.example.reportsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.EmailMessage;
import org.example.reportsystem.dto.SendEmailRequest;
import org.example.reportsystem.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RabbitTemplate rabbit;
    private final UserRepository users;

    @Value("${mailer.exchange}") String ex;        // email.exchange
    @Value("${mailer.routing-key}") String rk;     // email.send

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody SendEmailRequest req) {
        List<String> emails = users.findEmailsByIds(req.getUserIds());
        if (emails.isEmpty())
            return ResponseEntity.badRequest().body("no recipients");

        // общие переменные для шаблона
        Map<String,Object> baseVars = new HashMap<>();
        if (req.getVars() != null) baseVars.putAll(req.getVars());
        if (req.getUrl() != null && !req.getUrl().isBlank()) baseVars.put("url", req.getUrl());

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
    }

    private static List<String> emptyIfNull(List<String> l) {
        return l == null ? List.of() : l;
    }
}
