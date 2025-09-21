package org.example.reportsystem.repository;

import org.example.reportsystem.model.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignee","project"})
    List<Task> findAll();

    @EntityGraph(attributePaths = {"assignee","project"})
    List<Task> findByDeadlineBetween(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"assignee","project"})
    List<Task> findByAssignee_IdAndDeadlineBetween(Long assigneeId, LocalDateTime from, LocalDateTime to);
}
