package com.autodoc.backend.agent.strategy;

import com.autodoc.backend.agent.AgentStep;
import com.autodoc.backend.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

public class ReflectionStrategy implements PlanningStrategy {

    private static final String REACT_PREFIX = """
            Reasoning mode: ACTIVE.
            Before every tool call (cloneRepo, listFiles, readFile), \
            call the think tool first with one sentence explaining what you are about to do and why. Be concise.

            """;

    private static final String REVIEW_SYSTEM_PROMPT = """
            You are a senior technical writer reviewing a README draft for accuracy.

            You have access to the same repository tools (readFile, listFiles) to re-check any claims.

            Your job:
            1. Re-read 2-3 key files to verify the most important factual claims \
            (tech stack, setup commands, project purpose).
            2. Correct any wrong commands, wrong file paths, invented features, or missing sections.
            3. Output ONLY the corrected raw Markdown — no commentary, no explanation of your changes.

            If the draft is already accurate and complete, output it unchanged.
            """;

    @Override
    public StrategyResult execute(ChatClient chatClient, AgentTools agentTools, String systemPrompt, String goal) {
        // Call 1: generate draft with explicit reasoning
        ChatResponse draftResponse = chatClient.prompt()
                .system(REACT_PREFIX + systemPrompt)
                .user(goal)
                .tools(agentTools)
                .call()
                .chatResponse();
        var usage1 = draftResponse.getMetadata().getUsage();
        int inputTokens = usage1.getPromptTokens() != null ? usage1.getPromptTokens().intValue() : 0;
        int outputTokens = usage1.getCompletionTokens() != null ? usage1.getCompletionTokens().intValue() : 0;

        String draft = draftResponse.getResult().getOutput().getText();

        // Signal to the frontend that reflection is starting
        agentTools.emitStep(AgentStep.memory(
                "Reflecting on draft",
                "Reviewing README for accuracy",
                "Re-reading key files to verify claims…"
        ));

        // Truncate draft to keep Call 2 input tokens within rate limits
        String draftForReview = draft.length() > 6000 ? draft.substring(0, 6000) + "\n…[truncated]" : draft;

        // Call 2: self-review — Claude can re-read files to check its own claims
        ChatResponse reviewResponse = chatClient.prompt()
                .system(REVIEW_SYSTEM_PROMPT)
                .user("Original request: " + goal + "\n\nREADME draft to review:\n\n" + draftForReview)
                .tools(agentTools)
                .call()
                .chatResponse();
        var usage2 = reviewResponse.getMetadata().getUsage();
        inputTokens += usage2.getPromptTokens() != null ? usage2.getPromptTokens().intValue() : 0;
        outputTokens += usage2.getCompletionTokens() != null ? usage2.getCompletionTokens().intValue() : 0;

        return new StrategyResult(reviewResponse.getResult().getOutput().getText(), inputTokens, outputTokens);
    }
}
