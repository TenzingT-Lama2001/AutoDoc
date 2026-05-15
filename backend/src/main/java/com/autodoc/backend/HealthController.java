package com.autodoc.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {


    @GetMapping("/")
    public String home() {
        return "AutoDoc backend is running";
    }


    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
