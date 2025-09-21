package org.example.reportsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.TaskDto;
import org.example.reportsystem.dto.TaskUpsertDto;
import org.example.reportsystem.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskDto> getAll() {
        return taskService.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getById(@PathVariable Long id) {
        try { return ResponseEntity.ok(taskService.get(id)); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @PostMapping
    public TaskDto create(@RequestBody TaskUpsertDto body) {
        return taskService.create(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> update(@PathVariable Long id, @RequestBody TaskUpsertDto body) {
        try { return ResponseEntity.ok(taskService.update(id, body)); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
