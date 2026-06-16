package com.clip.server.report.repository;

import com.clip.server.report.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {
}
