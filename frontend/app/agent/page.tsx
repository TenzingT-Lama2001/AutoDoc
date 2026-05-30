"use client";

import { useRef, useState } from "react";

interface AgentStep {
  type: "LLM" | "TOOL";
  name: string;
  input: string | null;
  output: string;
  timestamp: string;
}

interface AgentRunResult {
  id: string;
  goal: string;
  steps: AgentStep[];
  status: "DONE" | "FAILED";
  result: string;
}

export default function AgentPage() {
  const [goal, setGoal] = useState("");
  const [steps, setSteps] = useState<AgentStep[]>([]);
  const [running, setRunning] = useState(false);
  const [finalResult, setFinalResult] = useState<AgentRunResult | null>(null);
  const esRef = useRef<EventSource | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  function runAgent() {
    if (!goal.trim() || running) return;

    esRef.current?.close();
    setSteps([]);
    setFinalResult(null);
    setRunning(true);

    const es = new EventSource(
      `${apiUrl}/agent/run?goal=${encodeURIComponent(goal)}`
    );
    esRef.current = es;

    es.addEventListener("step", (e) => {
      const step = JSON.parse(e.data) as AgentStep;
      setSteps((prev) => [...prev, step]);
    });

    es.addEventListener("done", (e) => {
      const run = JSON.parse(e.data) as AgentRunResult;
      setFinalResult(run);
      setRunning(false);
      es.close();
    });

    es.onerror = () => {
      setRunning(false);
      es.close();
    };
  }

  return (
    <main className="flex min-h-screen flex-col items-center bg-zinc-50 dark:bg-zinc-950 p-6">
      <div className="w-full max-w-2xl flex flex-col gap-5">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
            AutoDoc Agent
          </h1>
          <p className="text-sm text-zinc-500 mt-1">
            Give the agent a goal — it will use tools to figure it out step by step.
          </p>
        </div>

        <textarea
          className="w-full rounded-xl border border-zinc-200 bg-white p-4 text-zinc-900 shadow-sm outline-none focus:ring-2 focus:ring-zinc-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50 resize-none"
          rows={3}
          placeholder={`Try: "Read E:\\\\AutoDoc\\\\backend\\\\pom.xml and list all its dependencies"`}
          value={goal}
          onChange={(e) => setGoal(e.target.value)}
          disabled={running}
        />

        <button
          onClick={runAgent}
          disabled={running || !goal.trim()}
          className="self-end rounded-xl bg-zinc-900 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-zinc-700 disabled:opacity-40 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          {running ? "Running…" : "Run Agent"}
        </button>

        {steps.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold uppercase tracking-widest text-zinc-400">
              Steps
            </p>
            {steps.map((step, i) => (
              <div
                key={i}
                className={`rounded-lg border p-3 text-sm ${
                  step.type === "TOOL"
                    ? "border-blue-200 bg-blue-50 dark:border-blue-800 dark:bg-blue-950"
                    : "border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900"
                }`}
              >
                <div className="flex items-center gap-2 mb-1 flex-wrap">
                  <span
                    className={`text-xs font-mono font-bold px-1.5 py-0.5 rounded ${
                      step.type === "TOOL"
                        ? "bg-blue-200 text-blue-800 dark:bg-blue-800 dark:text-blue-200"
                        : "bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300"
                    }`}
                  >
                    {step.type}
                  </span>
                  <span className="font-medium text-zinc-700 dark:text-zinc-300">
                    {step.name}
                  </span>
                  {step.input && (
                    <span className="text-zinc-400 font-mono text-xs truncate max-w-xs">
                      {step.input}
                    </span>
                  )}
                </div>
                <p className="text-zinc-600 dark:text-zinc-400 text-xs whitespace-pre-wrap line-clamp-4">
                  {step.output}
                </p>
              </div>
            ))}
            {running && (
              <p className="text-xs text-zinc-400 animate-pulse">
                Agent is thinking…
              </p>
            )}
          </div>
        )}

        {finalResult && (
          <div
            className={`rounded-xl border p-5 ${
              finalResult.status === "DONE"
                ? "border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-950"
                : "border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-950"
            }`}
          >
            <p
              className={`text-xs font-semibold uppercase tracking-widest mb-2 ${
                finalResult.status === "DONE"
                  ? "text-green-600 dark:text-green-400"
                  : "text-red-600 dark:text-red-400"
              }`}
            >
              {finalResult.status === "DONE" ? "Result" : "Failed"}
            </p>
            <p className="text-zinc-800 dark:text-zinc-200 whitespace-pre-wrap leading-relaxed text-sm">
              {finalResult.result}
            </p>
          </div>
        )}
      </div>
    </main>
  );
}
