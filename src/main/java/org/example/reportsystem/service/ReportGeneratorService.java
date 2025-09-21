package org.example.reportsystem.service;

import org.example.reportsystem.model.Task;
import org.example.reportsystem.repository.TaskRepository;
import org.example.reportsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportGeneratorService {
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;

    public ReportGeneratorService(TaskRepository taskRepo, UserRepository userRepo) {
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
    }

    private static String statusLabel(String s) {
        if (s == null) return "UNSPECIFIED";
        return switch (s) {
            case "TODO" -> "Plan";
            case "IN_PROGRESS" -> "In progress";
            case "DONE" -> "Done";
            default -> s;
        };
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    public Generated build(Long userId, LocalDate fromDate, LocalDate toDate,
                           String preset, boolean onlyCompleted, boolean groupByStatus) {

        LocalDate from = Objects.requireNonNullElse(fromDate, LocalDate.now());
        LocalDate to   = Objects.requireNonNullElse(toDate, from);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(LocalTime.MAX);

        List<Task> tasks = (userId == null)
                ? taskRepo.findByDeadlineBetween(fromDt, toDt)
                : taskRepo.findByAssignee_IdAndDeadlineBetween(userId, fromDt, toDt);

        if (onlyCompleted) {
            tasks = tasks.stream().filter(Task::isCompleted).toList();
        }

        String userName = (userId == null) ? "Team" :
                userRepo.findById(userId)
                        .map(u -> Optional.ofNullable(u.getName())
                                .orElse(Optional.ofNullable(u.getUsername()).orElse("User #" + u.getId())))
                        .orElse("User #" + userId);

        String title = switch (preset == null ? "" : preset.toLowerCase()) {
            case "daily" -> "Daily report — " + userName + " — " + from;
            case "monthly" -> "Monthly report — " + userName + " — " + from.getYear() + "-"
                    + String.format("%02d", from.getMonthValue());
            default -> "Weekly report — " + userName + " — " + from + "…" + to;
        };

        StringBuilder sb = new StringBuilder();
        sb.append("Period: ").append(from).append(" — ").append(to).append("\n");
        sb.append("Owner: ").append(userName).append("\n");
        sb.append("Tasks total: ").append(tasks.size()).append("\n\n");

        if (tasks.isEmpty()) {
            sb.append("No tasks in the selected period.");
        } else if (groupByStatus) {
            // Key is String = enum name or "UNSPECIFIED"
            Map<String, List<Task>> byStatus = tasks.stream().collect(
                    Collectors.groupingBy(
                            t -> Optional.ofNullable(t.getStatus()).map(Enum::name).orElse("UNSPECIFIED"),
                            LinkedHashMap::new,
                            Collectors.toList()));

            for (Map.Entry<String, List<Task>> e : byStatus.entrySet()) {
                sb.append(statusLabel(e.getKey())).append(":\n");
                int i = 1;
                for (Task t : e.getValue()) {
                    String d = t.getDeadline() != null ? ", deadline " + t.getDeadline() : "";
                    String p = (t.getProject() != null && t.getProject().getName() != null)
                            ? ", project " + t.getProject().getName() : "";
                    String done = t.isCompleted() ? "✓" : "•";
                    sb.append(i++).append(". ").append(done).append(" ")
                            .append(Optional.ofNullable(t.getTitle()).orElse("(no title)"))
                            .append(p).append(d);
                    if (t.getDescription() != null && !t.getDescription().isBlank()) {
                        sb.append(" — ").append(sanitize(t.getDescription()));
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("Tasks:\n");
            int i = 1;
            for (Task t : tasks) {
                String d = t.getDeadline() != null ? ", deadline " + t.getDeadline() : "";
                String p = (t.getProject() != null && t.getProject().getName() != null)
                        ? ", project " + t.getProject().getName() : "";
                String done = t.isCompleted() ? "✓" : "•";
                sb.append(i++).append(". ").append(done).append(" ")
                        .append(Optional.ofNullable(t.getTitle()).orElse("(no title)"))
                        .append(p).append(d);
                if (t.getDescription() != null && !t.getDescription().isBlank()) {
                    sb.append(" — ").append(sanitize(t.getDescription()));
                }
                sb.append("\n");
            }
        }

        return new Generated(title, sb.toString());
    }

    public record Generated(String title, String content) {}
}
