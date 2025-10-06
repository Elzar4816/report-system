package org.example.reportsystem.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "deadline")
    private LocalDate deadline;
    @Column(precision = 14, scale = 2) // до 999 999 999 999.99
    private BigDecimal cost;
}
