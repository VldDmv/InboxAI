# InboxAI

Java 17 · Spring Boot 3.2 · PostgreSQL 16 · pgvector · Thymeleaf · Anthropic Claude

AI-powered triage for RSS feeds (and later IMAP mail). Pulls items from your
sources, classifies each one with Claude, generates a one-line summary, and
stores semantic embeddings in pgvector for "find similar items" search.

## Roadmap

- [x] Slice 1 — Spring Boot skeleton, health endpoint
- [x] Slice 2 — Liquibase + domain (User/Source/Item) + pgvector
- [ ] Slice 3 — RSS fetch (ROME) + scheduler + dedup
- [ ] Slice 4 — Anthropic client + classification with prompt caching
- [ ] Slice 5 — Embeddings + semantic similarity search
- [ ] Slice 6 — Thymeleaf inbox UI
- [ ] Slice 7 — Spring Security session auth
- [ ] Slice 8 — IMAP source + Google OAuth2 (optional)

## Local setup

Requirements: JDK 17+, Maven 3.9+, PostgreSQL 16+ with `vector` extension.

    createdb inboxai
    psql -d inboxai -c 'CREATE EXTENSION IF NOT EXISTS vector;'
    # Defaults: jdbc:postgresql://localhost:5432/inboxai, user=inboxai, pass=inboxai
    # Override with INBOXAI_DB_URL / INBOXAI_DB_USER / INBOXAI_DB_PASSWORD.

    mvn spring-boot:run
    # http://localhost:8080 → {"app":"InboxAI","status":"up","version":"..."}

Tests run against an in-memory H2 (PostgreSQL compatibility mode); Liquibase
is disabled in the `test` profile and Hibernate generates the schema from the
JPA entities.
