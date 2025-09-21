package org.example.reportsystem.repository;

import org.example.reportsystem.model.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Report> findAll();

    @Override
    @EntityGraph(attributePaths = {"user"})
    Optional<Report> findById(Long id);
}
