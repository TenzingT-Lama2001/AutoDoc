package com.autodoc.backend.trace;

import com.autodoc.backend.agent.AgentStep;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TraceRepository {

    private static final Logger log = LoggerFactory.getLogger(TraceRepository.class);
    private final JdbcTemplate jdbc;

    public TraceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_runs (
                    id            VARCHAR(36)  PRIMARY KEY,
                    repo_url      TEXT         NOT NULL,
                    strategy      VARCHAR(50),
                    status        VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
                    started_at    TIMESTAMPTZ  NOT NULL,
                    completed_at  TIMESTAMPTZ,
                    result        TEXT
                )""");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_steps (
                    id          BIGSERIAL   PRIMARY KEY,
                    run_id      VARCHAR(36) NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
                    step_order  INT         NOT NULL,
                    type        VARCHAR(20) NOT NULL,
                    name        TEXT,
                    input       TEXT,
                    output      TEXT,
                    timestamp   TIMESTAMPTZ NOT NULL
                )""");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_agent_steps_run_id ON agent_steps(run_id)");
    }

    public void insertRun(String runId, String repoUrl, String strategy) {
        try {
            jdbc.update(
                "INSERT INTO agent_runs (id, repo_url, strategy, started_at) VALUES (?, ?, ?, ?)",
                runId, repoUrl, strategy, Timestamp.from(Instant.now())
            );
        } catch (Exception e) {
            log.warn("Failed to insert run {}: {}", runId, e.getMessage());
        }
    }

    public void insertStep(String runId, AgentStep step, int stepOrder) {
        try {
            jdbc.update(
                "INSERT INTO agent_steps (run_id, step_order, type, name, input, output, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
                runId, stepOrder, step.type().name(), step.name(),
                step.input(), step.output(), Timestamp.from(step.timestamp())
            );
        } catch (Exception e) {
            log.warn("Failed to insert step for run {}: {}", runId, e.getMessage());
        }
    }

    public void completeRun(String runId, String status, String result) {
        try {
            jdbc.update(
                "UPDATE agent_runs SET status = ?, result = ?, completed_at = ? WHERE id = ?",
                status, result, Timestamp.from(Instant.now()), runId
            );
        } catch (Exception e) {
            log.warn("Failed to complete run {}: {}", runId, e.getMessage());
        }
    }

    public List<Map<String, Object>> listRuns() {
        return jdbc.queryForList("""
                SELECT r.id, r.repo_url, r.strategy, r.status,
                       r.started_at, r.completed_at,
                       EXTRACT(EPOCH FROM (COALESCE(r.completed_at, NOW()) - r.started_at))::int AS duration_seconds,
                       COUNT(s.id) AS step_count
                FROM agent_runs r
                LEFT JOIN agent_steps s ON s.run_id = r.id
                GROUP BY r.id
                ORDER BY r.started_at DESC
                """);
    }

    public Optional<Map<String, Object>> getRunById(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, repo_url, strategy, status, started_at, completed_at, result,
                       EXTRACT(EPOCH FROM (COALESCE(completed_at, NOW()) - started_at))::int AS duration_seconds
                FROM agent_runs WHERE id = ?
                """, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Map<String, Object>> getStepsByRunId(String runId) {
        return jdbc.queryForList(
            "SELECT step_order, type, name, input, output, timestamp FROM agent_steps WHERE run_id = ? ORDER BY step_order",
            runId
        );
    }
}
