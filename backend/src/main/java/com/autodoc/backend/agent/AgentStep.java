package com.autodoc.backend.agent;

import java.time.Instant;

public record AgentStep(
        StepType type,
        String name,
        String input,
        String output,
        Instant timestamp
) {
    public enum StepType { LLM, TOOL, MEMORY }

    public static AgentStep llm(String name, String output) {
        return new AgentStep(StepType.LLM, name, null, output, Instant.now());
    }

    public static AgentStep tool(String name, String input, String output) {
        return new AgentStep(StepType.TOOL, name, input, output, Instant.now());
    }

    public static AgentStep memory(String name, String query, String result) {
        return new AgentStep(StepType.MEMORY, name, query, result, Instant.now());
    }
}
