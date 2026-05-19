package com.inboxai.embedding;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcItemEmbeddingDao implements ItemEmbeddingDao {

    private final JdbcTemplate jdbc;

    public JdbcItemEmbeddingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UnembeddedItem> findUnembedded(int limit) {
        return jdbc.query(
                """
                SELECT id, title, COALESCE(content_text, '') AS content_text
                FROM items
                WHERE embedded_at IS NULL
                ORDER BY fetched_at ASC
                LIMIT ?
                """,
                (rs, i) -> new UnembeddedItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content_text")),
                limit);
    }

    @Override
    public void saveEmbedding(long itemId, float[] embedding) {
        jdbc.update(
                "UPDATE items SET embedding = ?::vector, embedded_at = now() WHERE id = ?",
                toVectorLiteral(embedding), itemId);
    }

    @Override
    public List<SimilarItem> findSimilarToVector(float[] queryVector, int limit) {
        String vec = toVectorLiteral(queryVector);
        return jdbc.query(
                """
                SELECT id, title, summary, link,
                       embedding <=> ?::vector AS distance
                FROM items
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> ?::vector ASC
                LIMIT ?
                """,
                SIMILAR_MAPPER, vec, vec, limit);
    }

    @Override
    public List<SimilarItem> findSimilarToItem(long itemId, int limit) {
        return jdbc.query(
                """
                SELECT i.id, i.title, i.summary, i.link,
                       i.embedding <=> (SELECT embedding FROM items WHERE id = ?) AS distance
                FROM items i
                WHERE i.id <> ?
                  AND i.embedding IS NOT NULL
                  AND (SELECT embedding FROM items WHERE id = ?) IS NOT NULL
                ORDER BY distance ASC
                LIMIT ?
                """,
                SIMILAR_MAPPER, itemId, itemId, itemId, limit);
    }

    private static final RowMapper<SimilarItem> SIMILAR_MAPPER = (rs, i) -> new SimilarItem(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("summary"),
            rs.getString("link"),
            rs.getDouble("distance"));

    static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
