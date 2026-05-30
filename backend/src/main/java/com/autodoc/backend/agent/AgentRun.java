package com.autodoc.backend.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class AgentRun {

    public enum Status { RUNNING, DONE, FAILED }

    private final String id = UUID.randomUUID().toString();
    private final String goal;
    private final List<AgentStep> steps = new ArrayList<>();
    private Status status = Status.RUNNING;
    private String result;

    @JsonIgnore
    private Consumer<AgentStep> stepListener;

    public AgentRun(String goal) {
        this.goal = goal;
    }

    public void setStepListener(Consumer<AgentStep> listener) {
        this.stepListener = listener;
    }

    public synchronized void addStep(AgentStep step) {
        steps.add(step);
        if (stepListener != null) stepListener.accept(step);
    }

    public void complete(String result) {
        this.result = result;
        this.status = Status.DONE;
    }

    public void fail(String reason) {
        this.result = reason;
        this.status = Status.FAILED;
    }

    public String getId()                       { return id; }
    public String getGoal()                     { return goal; }
    public List<AgentStep> getSteps()           { return Collections.unmodifiableList(steps); }
    public Status getStatus()                   { return status; }
    public String getResult()                   { return result; }
}
