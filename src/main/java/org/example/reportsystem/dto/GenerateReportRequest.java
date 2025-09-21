package org.example.reportsystem.dto;

import java.time.LocalDate;

public record GenerateReportRequest(
        Long userId,
        LocalDate fromDate,
        LocalDate toDate,
        String preset,
        Boolean onlyCompleted,
        Boolean groupByStatus
) {}
