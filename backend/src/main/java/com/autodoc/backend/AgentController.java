package com.autodoc.backend;

import com.autodoc.backend.agent.Agent;
import com.autodoc.backend.agent.AgentRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final Agent agent;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentController(Agent agent, ObjectMapper objectMapper) {
        this.agent = agent;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@RequestParam String goal) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executor.submit(() -> {
            AgentRun agentRun = new AgentRun(goal);

            agentRun.setStepListener(step -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(step)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            agent.run(agentRun);

            try {
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(objectMapper.writeValueAsString(agentRun)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
