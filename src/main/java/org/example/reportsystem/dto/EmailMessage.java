// package org.example.reportsystem.dto;
package org.example.reportsystem.dto;

import java.util.List;
import java.util.Map;

public record EmailMessage(
        String messageId,
        List<String> to,
        String subject,
        String template,
        Map<String,Object> vars,
        List<String> cc,
        List<String> bcc
) {}
