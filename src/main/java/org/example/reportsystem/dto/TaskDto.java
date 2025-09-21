// TaskDto.java
package org.example.reportsystem.dto;

public record TaskDto(
        Long id,
        String title,
        String description,
        String status,
        String deadline,       // ISO-8601
        Boolean completed,
        Long assigneeId,
        String assigneeName,
        Long projectId,
        String projectName
) {}
