// TaskUpsertDto.java
package org.example.reportsystem.dto;

public record TaskUpsertDto(
        String title,
        String description,
        String status,
        String deadline,
        Boolean completed,
        Long assigneeId,
        Long projectId
) {}
