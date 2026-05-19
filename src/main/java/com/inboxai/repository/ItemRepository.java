package com.inboxai.repository;

import com.inboxai.domain.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySourceIdAndGuid(Long sourceId, String guid);

    boolean existsBySourceIdAndGuid(Long sourceId, String guid);

    List<Item> findByClassifiedAtIsNullOrderByFetchedAtAsc(Pageable pageable);
}
