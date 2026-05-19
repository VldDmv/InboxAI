package com.inboxai.embedding;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemEmbeddingServiceTest {

    private final VoyageEmbeddingClient client = mock(VoyageEmbeddingClient.class);
    private final ItemEmbeddingDao dao = mock(ItemEmbeddingDao.class);
    private final ItemEmbeddingService service = new ItemEmbeddingService(client, dao);

    @Test
    void buildsTextFromTitleAndContentAndPersistsEmbedding() {
        float[] vector = {0.1f, 0.2f};
        when(client.embed(any(), eq(VoyageEmbeddingClient.InputType.DOCUMENT))).thenReturn(vector);

        service.embed(new UnembeddedItem(42L, "Hello", "world"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client).embed(text.capture(), eq(VoyageEmbeddingClient.InputType.DOCUMENT));
        assertThat(text.getValue()).isEqualTo("Hello\nworld");
        verify(dao).saveEmbedding(42L, vector);
    }

    @Test
    void skipsEmbeddingWhenBothTitleAndContentAreBlank() {
        service.embed(new UnembeddedItem(1L, "  ", null));

        verifyNoInteractions(client);
        verify(dao, never()).saveEmbedding(anyLongMatcher(), any());
    }

    @Test
    void truncatesLongContent() {
        String longContent = "x".repeat(20_000);
        when(client.embed(any(), any())).thenReturn(new float[]{0.0f});

        service.embed(new UnembeddedItem(7L, "T", longContent));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(client).embed(text.capture(), eq(VoyageEmbeddingClient.InputType.DOCUMENT));
        assertThat(text.getValue().length()).isLessThanOrEqualTo(8000);
    }

    private static long anyLongMatcher() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
