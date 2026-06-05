package com.clip.server.chat.repository;

import com.clip.server.chat.entity.UserWeakness;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWeaknessRepository extends JpaRepository<UserWeakness, Long> {
}
