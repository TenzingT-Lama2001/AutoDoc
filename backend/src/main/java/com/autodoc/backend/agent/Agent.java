package com.autodoc.backend.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Runs the agent loop:
 *   LLM decides → requests tool → tool executes → result back to LLM → repeat
 * Spring AI handles the multi-turn loop internally; we observe each tool call
 * via AgentTools.stepSink (ThreadLocal) and record LLM start/end as steps.
 */
@Service
public class Agent {

    private final ChatClient chatClient;
    private final AgentTools agentTools;

    public Agent(ChatClient.Builder builder, AgentTools agentTools) {
        this.chatClient = builder.build();
        this.agentTools = agentTools;
    }

    public void run(AgentRun agentRun) {
        AgentTools.stepSink.set(agentRun::addStep);
        try {
            agentRun.addStep(AgentStep.llm("Agent started", "Goal: " + agentRun.getGoal()));

            String result = chatClient.prompt()
                    .system("""
                            You are a helpful technical assistant with access to filesystem tools.
                            Use tools to gather the information you need to answer accurately.
                            Once you have enough information, give a clear and complete answer.
                            """)
                    .user(agentRun.getGoal())
                    .tools(agentTools)
                    .call()
                    .content();

            agentRun.addStep(AgentStep.llm("Agent completed", result));
            agentRun.complete(result);

        } catch (Exception e) {
            agentRun.fail(e.getMessage());
        } finally {
            AgentTools.stepSink.remove();
        }
    }
}
