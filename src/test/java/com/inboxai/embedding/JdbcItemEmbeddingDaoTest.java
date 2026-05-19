package com.inboxai.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcItemEmbeddingDaoTest {

    @Test
    void toVectorLiteralProducesPgvectorTextFormat() {
        String literal = JdbcItemEmbeddingDao.toVectorLiteral(new float[]{0.1f, -0.25f, 1.0f});
        assertThat(literal).isEqualTo("[0.1,-0.25,1.0]");
    }

    @Test
    void toVectorLiteralHandlesEmptyArray() {
        assertThat(JdbcItemEmbeddingDao.toVectorLiteral(new float[]{})).isEqualTo("[]");
    }

    @Test
    void toVectorLiteralHandlesSingleElement() {
        assertThat(JdbcItemEmbeddingDao.toVectorLiteral(new float[]{0.5f})).isEqualTo("[0.5]");
    }
}
