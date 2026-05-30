package com.autodoc.backend.agent.tools;

import com.autodoc.backend.agent.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ListFilesTool implements Tool {

    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "target", "build", ".gradle", "__pycache__", ".idea", ".vscode"
    );

    @Override
    public String name() { return "listFiles"; }

    @Override
    public String description() {
        return "List all files in a directory recursively. " +
               "Returns a newline-separated list of relative file paths. " +
               "Skips common noise directories like .git, node_modules, target, build.";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String pathStr = (String) args.get("path");
        if (pathStr == null || pathStr.isBlank()) return "Error: 'path' argument is required.";

        Path root = Path.of(pathStr);
        if (!Files.isDirectory(root)) return "Error: '" + pathStr + "' is not a directory.";

        try (Stream<Path> stream = Files.walk(root)) {
            String listing = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        for (Path part : p) {
                            if (SKIP_DIRS.contains(part.toString())) return false;
                        }
                        return true;
                    })
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.joining("\n"));

            return listing.isBlank() ? "No files found." : listing;
        } catch (IOException e) {
            return "Error listing files in '" + pathStr + "': " + e.getMessage();
        }
    }
}
