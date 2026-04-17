package com.clip.server.subtitle.repository;

import com.clip.server.subtitle.entity.Subtitle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
}
