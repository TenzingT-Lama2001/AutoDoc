package com.autodoc.backend.agent.strategy;

import com.autodoc.backend.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;

public interface PlanningStrategy {
    StrategyResult execute(ChatClient chatClient, AgentTools agentTools, String systemPrompt, String goal);
}
