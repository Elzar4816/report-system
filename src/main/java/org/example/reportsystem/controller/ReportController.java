package org.example.reportsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.GenerateReportRequest;
import org.example.reportsystem.dto.GenerateReportResponse;
import org.example.reportsystem.model.Report;
import org.example.reportsystem.model.User;
import org.example.reportsystem.repository.ReportRepository;
import org.example.reportsystem.repository.UserRepository;
import org.example.reportsystem.service.ReportGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final ReportGeneratorService generator;
    private final UserRepository userRepository;

    @GetMapping
    public List<Report> getAll() {
        return reportRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getById(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(report -> {
                    reportRepository.delete(report);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/preview")
    public GenerateReportResponse preview(@RequestBody GenerateReportRequest req) {
        var gen = generator.build(
                req.userId(),
                req.fromDate(),
                req.toDate(),
                req.preset(),
                Boolean.TRUE.equals(req.onlyCompleted()),
                Boolean.TRUE.equals(req.groupByStatus())
        );
        return new GenerateReportResponse(gen.title(), gen.content());
    }

    @PostMapping("/generate")
    public Report generateAndSave(@RequestBody GenerateReportRequest req) {
        var gen = generator.build(
                req.userId(),
                req.fromDate(),
                req.toDate(),
                req.preset(),
                Boolean.TRUE.equals(req.onlyCompleted()),
                Boolean.TRUE.equals(req.groupByStatus())
        );

        Report r = new Report();
        r.setTitle(gen.title());
        r.setContent(gen.content());
        r.setDate(req.toDate() != null ? req.toDate() : LocalDate.now());

        if (req.userId() != null) {
            User u = userRepository.findById(req.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userId()));
            r.setUser(u);
        }
        return reportRepository.save(r);
    }
}
