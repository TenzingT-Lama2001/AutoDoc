package com.autodoc.backend.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public MemoryManager(Optional<VectorStore> vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore.orElse(null);
        this.jdbcTemplate = jdbcTemplate;
        if (this.vectorStore == null) {
            log.info("No VectorStore configured — long-term memory disabled");
        }
    }

    public void storeMemory(String runId, String repoUrl, String summary) {
        if (vectorStore == null || summary == null || summary.isBlank()) return;
        try {
            // Delete any existing entry for this repo before storing the updated one
            int deleted = jdbcTemplate.update(
                    "DELETE FROM vector_store WHERE metadata->>'repoUrl' = ?", repoUrl);
            if (deleted > 0) {
                log.debug("Replaced {} existing memory row(s) for {}", deleted, repoUrl);
            }
            Document doc = new Document(summary, Map.of("runId", runId, "repoUrl", repoUrl));
            vectorStore.add(List.of(doc));
        } catch (Exception e) {
            log.warn("Failed to store memory for run {}: {}", runId, e.getMessage());
        }
    }

    public List<String> recall(String query, int topK) {
        if (vectorStore == null || query == null || query.isBlank()) return List.of();
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).similarityThreshold(0.5).build()
            ).stream().map(Document::getText).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to recall memory for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
