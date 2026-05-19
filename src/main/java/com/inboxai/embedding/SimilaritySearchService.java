package com.inboxai.embedding;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimilaritySearchService {

    private final VoyageEmbeddingClient client;
    private final ItemEmbeddingDao dao;

    public SimilaritySearchService(VoyageEmbeddingClient client, ItemEmbeddingDao dao) {
        this.client = client;
        this.dao = dao;
    }

    public List<SimilarItem> searchByText(String query, long userId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        float[] vector = client.embed(query, VoyageEmbeddingClient.InputType.QUERY);
        return dao.findSimilarToVector(vector, userId, limit);
    }

    public List<SimilarItem> searchByItem(long itemId, long userId, int limit) {
        return dao.findSimilarToItem(itemId, userId, limit);
    }
}
