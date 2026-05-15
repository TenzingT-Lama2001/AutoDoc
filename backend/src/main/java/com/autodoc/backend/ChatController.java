package com.autodoc.backend;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    record ChatRequest(String prompt) {}
    record ChatResponse(String response) {}

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        String content = chatClient.prompt()
                .user(req.prompt())
                .call()
                .content();
        return new ChatResponse(content);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
