package com.autodoc.backend.agent;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    String execute(Map<String, Object> args);
}
