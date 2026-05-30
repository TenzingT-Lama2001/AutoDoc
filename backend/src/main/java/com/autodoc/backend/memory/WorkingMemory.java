package com.autodoc.backend.memory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkingMemory {

    private final Map<String, String> data = new LinkedHashMap<>();

    public void put(String key, String value) {
        data.put(key, value);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(data);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public String toPromptContext() {
        if (data.isEmpty()) return "";
        var sb = new StringBuilder("\n## Working Memory (key findings so far)\n");
        data.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}
