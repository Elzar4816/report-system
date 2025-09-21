// TaskService.java
package org.example.reportsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.TaskDto;
import org.example.reportsystem.dto.TaskMapper;
import org.example.reportsystem.dto.TaskUpsertDto;
import org.example.reportsystem.model.Project;
import org.example.reportsystem.model.Task;
import org.example.reportsystem.model.User;
import org.example.reportsystem.repository.ProjectRepository;
import org.example.reportsystem.repository.TaskRepository;
import org.example.reportsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;

    @Transactional(readOnly = true)
    public List<TaskDto> list() {
        return taskRepo.findAll().stream().map(TaskMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id) {
        var t = taskRepo.findById(id).orElseThrow();
        return TaskMapper.toDto(t);
    }

    @Transactional
    public TaskDto create(TaskUpsertDto dto) {
        var t = new Task();
        TaskMapper.applyUpsert(t, dto);
        if (dto.assigneeId() != null) {
            User u = userRepo.findById(dto.assigneeId()).orElseThrow();
            t.setAssignee(u);
        } else t.setAssignee(null);

        if (dto.projectId() != null) {
            Project p = projectRepo.findById(dto.projectId()).orElseThrow();
            t.setProject(p);
        } else t.setProject(null);

        return TaskMapper.toDto(taskRepo.save(t));
    }

    @Transactional
    public TaskDto update(Long id, TaskUpsertDto dto) {
        var t = taskRepo.findById(id).orElseThrow();
        TaskMapper.applyUpsert(t, dto);

        if (dto.assigneeId() != null) {
            t.setAssignee(userRepo.findById(dto.assigneeId()).orElseThrow());
        } else t.setAssignee(null);

        if (dto.projectId() != null) {
            t.setProject(projectRepo.findById(dto.projectId()).orElseThrow());
        } else t.setProject(null);

        return TaskMapper.toDto(taskRepo.save(t));
    }

    @Transactional
    public void delete(Long id) {
        taskRepo.deleteById(id);
    }
}
