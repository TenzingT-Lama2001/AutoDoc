package com.autodoc.backend.agent.strategy;

import com.autodoc.backend.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;

public class DefaultStrategy implements PlanningStrategy {

    @Override
    public String execute(ChatClient chatClient, AgentTools agentTools, String systemPrompt, String goal) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(goal)
                .tools(agentTools)
                .call()
                .content();
    }
}
