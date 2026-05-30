package com.autodoc.backend.agent.tools;

import com.autodoc.backend.agent.Tool;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

@Component
public class CloneRepoTool implements Tool {

    @Override
    public String name() { return "cloneRepo"; }

    @Override
    public String description() {
        return "Clone a public GitHub repository to a temporary local directory. " +
               "Returns the absolute path to the cloned directory.";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String url = (String) args.get("url");
        if (url == null || url.isBlank()) return "Error: 'url' argument is required.";

        try {
            Path tempDir = Files.createTempDirectory("autodoc-");
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(tempDir.toFile())
                    .setDepth(1)
                    .call()
                    .close();
            return tempDir.toAbsolutePath().toString();
        } catch (Exception e) {
            return "Error cloning repository '" + url + "': " + e.getMessage();
        }
    }

    public static void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }
}
