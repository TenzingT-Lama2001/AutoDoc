package com.autodoc.backend.agent.strategy;

import com.autodoc.backend.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;

public class ReActStrategy implements PlanningStrategy {

    private static final String REACT_PREFIX = """
            Reasoning mode: ACTIVE.
            Before every tool call (cloneRepo, listFiles, readFile), \
            call the think tool first with one sentence explaining what you are about to do and why. Be concise.

            """;

    @Override
    public String execute(ChatClient chatClient, AgentTools agentTools, String systemPrompt, String goal) {
        return chatClient.prompt()
                .system(REACT_PREFIX + systemPrompt)
                .user(goal)
                .tools(agentTools)
                .call()
                .content();
    }
}
