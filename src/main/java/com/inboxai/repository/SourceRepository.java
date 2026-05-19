package com.inboxai.repository;

import com.inboxai.domain.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<Source, Long> {
    List<Source> findByUserId(Long userId);

    List<Source> findByActiveTrue();
}
