// package org.example.reportsystem.dto;
package org.example.reportsystem.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SendEmailRequest {
    private List<Long> userIds;
    private String subject;
    private String template = "report_ready";
    private Map<String, Object> vars;
    private String url;
    private boolean personalize = false;
    private List<String> cc;
    private List<String> bcc;
}
