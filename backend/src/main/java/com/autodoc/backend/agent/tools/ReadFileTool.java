package com.autodoc.backend.agent.tools;

import com.autodoc.backend.agent.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class ReadFileTool implements Tool {

    @Override
    public String name() {
        return "readFile";
    }

    @Override
    public String description() {
        return "Read the full text contents of a file from the local filesystem. " +
               "Provide the absolute path to the file.";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String path = (String) args.get("path");
        if (path == null || path.isBlank()) return "Error: 'path' argument is required.";
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            return "Error reading file '" + path + "': " + e.getMessage();
        }
    }
}
