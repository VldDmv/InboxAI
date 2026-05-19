package com.inboxai.embedding;

import java.util.List;

public interface ItemEmbeddingDao {

    List<UnembeddedItem> findUnembedded(int limit);

    void saveEmbedding(long itemId, float[] embedding);

    List<SimilarItem> findSimilarToVector(float[] queryVector, long userId, int limit);

    List<SimilarItem> findSimilarToItem(long itemId, long userId, int limit);
}
