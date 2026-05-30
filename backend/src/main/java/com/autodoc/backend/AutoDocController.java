package com.autodoc.backend;

import com.autodoc.backend.agent.Agent;
import com.autodoc.backend.agent.AgentRun;
import com.autodoc.backend.agent.AgentTools;
import com.autodoc.backend.agent.AgentStep;
import com.autodoc.backend.memory.MemoryManager;
import com.autodoc.backend.memory.WorkingMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/autodoc")
public class AutoDocController {

    private static final String BASE_SYSTEM_PROMPT = """
            You are an expert technical writer and software engineer specialised in writing README files.

            Your job is to explore a cloned GitHub repository and produce a complete, professional README.md.

            Follow this process:
            1. Call listFiles on the repo root to understand the project structure.
            2. Read key files to understand the project: README (if any), package.json / pom.xml / build.gradle / Cargo.toml / pyproject.toml, main source files, config files.
            3. Identify: project name, purpose, tech stack, how to install, how to run, how to test, any notable architecture decisions.
            4. As you discover key facts call updateWorkingMemory — record projectName, techStack, mainPurpose, architectureNotes.
            5. Optionally call recallMemory early if the project type seems familiar — it may return useful context from past similar repos.
            6. Write a README.md with these sections (omit any that genuinely don't apply):
               - Project name and one-line description
               - Badges (optional, only if you can infer them)
               - Overview (2–4 sentences on what it does and why)
               - Tech Stack
               - Getting Started (Prerequisites, Installation, Running)
               - Usage (with examples if you can infer them)
               - Project Structure (a short annotated tree of key files/dirs)
               - Contributing (brief)
               - License (only if LICENSE file exists)

            Rules:
            - Be specific. Use real names, real commands, real file paths from the repo.
            - Do not invent features or commands you cannot verify from the code.
            - Output ONLY the raw Markdown for the README — no commentary before or after it.
            """;

    private final ChatClient chatClient;
    private final AgentTools agentTools;
    private final ObjectMapper objectMapper;
    private final MemoryManager memoryManager;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AutoDocController(ChatClient.Builder builder, AgentTools agentTools,
                             ObjectMapper objectMapper, MemoryManager memoryManager) {
        this.chatClient = builder.build();
        this.agentTools = agentTools;
        this.objectMapper = objectMapper;
        this.memoryManager = memoryManager;
    }

    @GetMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@RequestParam String repoUrl) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        executor.submit(() -> {
            AgentRun agentRun = new AgentRun(repoUrl);
            WorkingMemory workingMemory = new WorkingMemory();

            agentRun.setStepListener(step -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(step)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            AgentTools.stepSink.set(agentRun::addStep);
            AgentTools.workingMemorySink.set(workingMemory);
            try {
                agentRun.addStep(AgentStep.llm("AutoDoc started", "Generating README for: " + repoUrl));

                // Retrieve relevant memories from past similar runs and inject into prompt
                List<String> pastMemories = memoryManager.recall(repoUrl, 3);
                String systemPrompt = BASE_SYSTEM_PROMPT;
                if (!pastMemories.isEmpty()) {
                    String memoryContext = "\n\n## Context recalled from similar past projects\n"
                            + pastMemories.stream()
                                    .map(m -> "- " + m.replace("\n", " "))
                                    .collect(Collectors.joining("\n"));
                    systemPrompt = BASE_SYSTEM_PROMPT + memoryContext;
                    agentRun.addStep(AgentStep.memory("Past context loaded",
                            repoUrl, pastMemories.size() + " relevant memory/memories injected into prompt"));
                }

                String result = chatClient.prompt()
                        .system(systemPrompt)
                        .user("Generate a professional README.md for this GitHub repository: " + repoUrl)
                        .tools(agentTools)
                        .call()
                        .content();

                agentRun.addStep(AgentStep.llm("AutoDoc completed", result));
                agentRun.complete(result);

                // Store what the agent learned as long-term memory for future runs
                String memorySummary = buildMemorySummary(workingMemory, repoUrl);
                memoryManager.storeMemory(agentRun.getId(), repoUrl, memorySummary);

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(objectMapper.writeValueAsString(agentRun)));
                emitter.complete();

            } catch (Exception e) {
                agentRun.fail(e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data(objectMapper.writeValueAsString(agentRun)));
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(e);
                }
            } finally {
                AgentTools.stepSink.remove();
                AgentTools.workingMemorySink.remove();
            }
        });

        return emitter;
    }

    private String buildMemorySummary(WorkingMemory wm, String repoUrl) {
        if (wm.isEmpty()) return "Repository: " + repoUrl;
        StringBuilder sb = new StringBuilder("Repository: ").append(repoUrl).append("\n");
        wm.getAll().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}
