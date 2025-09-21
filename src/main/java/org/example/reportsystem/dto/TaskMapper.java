// TaskMapper.java
package org.example.reportsystem.dto;

import org.example.reportsystem.model.Task;
import org.example.reportsystem.model.Status;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public final class TaskMapper {
    private TaskMapper() {}

    public static TaskDto toDto(Task t) {
        var u = t.getAssignee();
        var p = t.getProject();
        return new TaskDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getDeadline() != null ? t.getDeadline().toString() : null,
                t.isCompleted(),
                u != null ? u.getId() : null,
                u != null ? (u.getName() != null ? u.getName() : u.getUsername()) : null,
                p != null ? p.getId() : null,
                p != null ? p.getName() : null
        );
    }

    public static void applyUpsert(Task t, TaskUpsertDto dto) {
        t.setTitle(dto.title());
        t.setDescription(dto.description());
        t.setStatus(dto.status() != null ? Status.valueOf(dto.status()) : null);


        if (dto.deadline() != null) {
            LocalDateTime dt;
            if (dto.deadline().endsWith("Z") || dto.deadline().contains("+") || dto.deadline().contains("-")) {
                dt = OffsetDateTime.parse(dto.deadline()).toLocalDateTime();
            } else {
                dt = LocalDateTime.parse(dto.deadline()); // ISO_LOCAL_DATE_TIME
            }
            t.setDeadline(dt);
        } else {
            t.setDeadline(null);
        }

        t.setCompleted(Boolean.TRUE.equals(dto.completed()));
    }
}
