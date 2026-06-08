package com.autodoc.backend.trace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/runs")
public class TraceController {

    record StepRecord(int stepOrder, String type, String name, String input, String output, Instant timestamp) {}

    record RunSummary(String id, String repoUrl, String strategy, String status,
                      Instant startedAt, Instant completedAt, int durationSeconds, long stepCount) {}

    record RunDetail(String id, String repoUrl, String strategy, String status,
                     Instant startedAt, Instant completedAt, int durationSeconds,
                     String result, List<StepRecord> steps) {}

    private final TraceRepository repo;

    public TraceController(TraceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<RunSummary> listRuns() {
        return repo.listRuns().stream().map(this::toSummary).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RunDetail> getRun(@PathVariable String id) {
        Optional<Map<String, Object>> run = repo.getRunById(id);
        if (run.isEmpty()) return ResponseEntity.notFound().build();
        List<StepRecord> steps = repo.getStepsByRunId(id).stream().map(this::toStep).toList();
        return ResponseEntity.ok(toDetail(run.get(), steps));
    }

    private RunSummary toSummary(Map<String, Object> row) {
        return new RunSummary(
            (String) row.get("id"),
            (String) row.get("repo_url"),
            (String) row.get("strategy"),
            (String) row.get("status"),
            toInstant(row.get("started_at")),
            toInstant(row.get("completed_at")),
            toInt(row.get("duration_seconds")),
            toLong(row.get("step_count"))
        );
    }

    private RunDetail toDetail(Map<String, Object> row, List<StepRecord> steps) {
        return new RunDetail(
            (String) row.get("id"),
            (String) row.get("repo_url"),
            (String) row.get("strategy"),
            (String) row.get("status"),
            toInstant(row.get("started_at")),
            toInstant(row.get("completed_at")),
            toInt(row.get("duration_seconds")),
            (String) row.get("result"),
            steps
        );
    }

    private StepRecord toStep(Map<String, Object> row) {
        return new StepRecord(
            toInt(row.get("step_order")),
            (String) row.get("type"),
            (String) row.get("name"),
            (String) row.get("input"),
            (String) row.get("output"),
            toInstant(row.get("timestamp"))
        );
    }

    private Instant toInstant(Object val) {
        if (val instanceof Timestamp ts) return ts.toInstant();
        return null;
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}
