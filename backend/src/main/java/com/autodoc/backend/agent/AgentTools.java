package com.autodoc.backend.agent;

import com.autodoc.backend.agent.tools.CloneRepoTool;
import com.autodoc.backend.agent.tools.ListFilesTool;
import com.autodoc.backend.agent.tools.ReadFileTool;
import com.autodoc.backend.memory.MemoryManager;
import com.autodoc.backend.memory.WorkingMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Spring AI adapter. Exposes our Tool implementations as @Tool-annotated methods
 * so ChatClient can discover and call them during the agent loop.
 * Step tracking is wired via a ThreadLocal so each request gets its own AgentRun.
 */
@Service
public class AgentTools {

    public static final ThreadLocal<Consumer<AgentStep>> stepSink = new ThreadLocal<>();
    public static final ThreadLocal<WorkingMemory> workingMemorySink = new ThreadLocal<>();

    private final ReadFileTool readFileTool;
    private final CloneRepoTool cloneRepoTool;
    private final ListFilesTool listFilesTool;
    private final MemoryManager memoryManager;

    public AgentTools(ReadFileTool readFileTool, CloneRepoTool cloneRepoTool,
                      ListFilesTool listFilesTool, MemoryManager memoryManager) {
        this.readFileTool = readFileTool;
        this.cloneRepoTool = cloneRepoTool;
        this.listFilesTool = listFilesTool;
        this.memoryManager = memoryManager;
    }

    @Tool(description = "Read the full text contents of a file from the local filesystem. Provide the absolute path to the file.")
    public String readFile(String path) {
        String result = readFileTool.execute(Map.of("path", path));
        emitTool("readFile", path, result);
        return result;
    }

    @Tool(description = "Clone a public GitHub repository to a temporary local directory. Returns the absolute path to the cloned directory.")
    public String cloneRepo(String url) {
        String result = cloneRepoTool.execute(Map.of("url", url));
        emitTool("cloneRepo", url, result);
        return result;
    }

    @Tool(description = "List all files in a directory recursively. Returns a newline-separated list of relative file paths. Skips .git, node_modules, target, build.")
    public String listFiles(String path) {
        String result = listFilesTool.execute(Map.of("path", path));
        emitTool("listFiles", path, result);
        return result;
    }

    @Tool(description = "Store a key finding in working memory for this run. Use for: projectName, techStack, mainPurpose, keyDependencies, architectureNotes, setupInstructions.")
    public String updateWorkingMemory(String key, String value) {
        WorkingMemory wm = workingMemorySink.get();
        if (wm != null) wm.put(key, value);
        emitTool("updateWorkingMemory", key + "=" + value, "stored");
        return "Stored: " + key + " = " + value;
    }

    @Tool(description = "Search long-term memory for relevant context from past README generations on similar repositories. Call this early if the project type seems familiar.")
    public String recallMemory(String query) {
        List<String> memories = memoryManager.recall(query, 3);
        if (memories.isEmpty()) return "No relevant memories found.";
        String result = String.join("\n---\n", memories);
        emitMemory("recallMemory", query, result);
        return result;
    }

    private void emitTool(String toolName, String input, String output) {
        Consumer<AgentStep> sink = stepSink.get();
        if (sink == null) return;
        String preview = output.length() > 500 ? output.substring(0, 500) + "…" : output;
        sink.accept(AgentStep.tool(toolName, input, preview));
    }

    private void emitMemory(String name, String query, String output) {
        Consumer<AgentStep> sink = stepSink.get();
        if (sink == null) return;
        String preview = output.length() > 500 ? output.substring(0, 500) + "…" : output;
        sink.accept(AgentStep.memory(name, query, preview));
    }
}
