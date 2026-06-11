package com.autodoc.backend.agent.strategy;

import com.autodoc.backend.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

public class ReActStrategy implements PlanningStrategy {

    private static final String REACT_PREFIX = """
            Reasoning mode: ACTIVE.
            Before every tool call (cloneRepo, listFiles, readFile), \
            call the think tool first with one sentence explaining what you are about to do and why. Be concise.

            """;

    @Override
    public StrategyResult execute(ChatClient chatClient, AgentTools agentTools, String systemPrompt, String goal) {
        ChatResponse response = chatClient.prompt()
                .system(REACT_PREFIX + systemPrompt)
                .user(goal)
                .tools(agentTools)
                .call()
                .chatResponse();
        var usage = response.getMetadata().getUsage();
        int inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0;
        int outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : 0;
        return new StrategyResult(response.getResult().getOutput().getText(), inputTokens, outputTokens);
    }
}
