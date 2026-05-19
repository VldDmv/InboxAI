package com.inboxai.repository;

import com.inboxai.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySourceIdAndGuid(Long sourceId, String guid);

    boolean existsBySourceIdAndGuid(Long sourceId, String guid);
}
