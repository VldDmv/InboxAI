package com.inboxai.repository;

import com.inboxai.domain.Category;
import com.inboxai.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySourceIdAndGuid(Long sourceId, String guid);

    boolean existsBySourceIdAndGuid(Long sourceId, String guid);

    List<Item> findByClassifiedAtIsNullOrderByFetchedAtAsc(Pageable pageable);

    @Query(
            value = """
                    SELECT i FROM Item i JOIN FETCH i.source s
                    WHERE s.user.id = :userId
                      AND (:category IS NULL OR i.category = :category)
                    ORDER BY COALESCE(i.publishedAt, i.fetchedAt) DESC
                    """,
            countQuery = """
                    SELECT count(i) FROM Item i
                    WHERE i.source.user.id = :userId
                      AND (:category IS NULL OR i.category = :category)
                    """
    )
    Page<Item> findFeed(
            @Param("userId") Long userId,
            @Param("category") Category category,
            Pageable pageable);
}
