"use client";

import { useRef, useState } from "react";

interface AgentStep {
  type: "LLM" | "TOOL" | "MEMORY";
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

export default function AutoDocPage() {
  const [repoUrl, setRepoUrl] = useState("");
  const [steps, setSteps] = useState<AgentStep[]>([]);
  const [running, setRunning] = useState(false);
  const [finalResult, setFinalResult] = useState<AgentRunResult | null>(null);
  const [copied, setCopied] = useState(false);
  const esRef = useRef<EventSource | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  function generate() {
    if (!repoUrl.trim() || running) return;

    esRef.current?.close();
    setSteps([]);
    setFinalResult(null);
    setCopied(false);
    setRunning(true);

    const es = new EventSource(
      `${apiUrl}/autodoc/run?repoUrl=${encodeURIComponent(repoUrl)}`
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

  function copyReadme() {
    if (!finalResult?.result) return;
    navigator.clipboard.writeText(finalResult.result).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  return (
    <main className="flex min-h-screen flex-col items-center bg-zinc-50 dark:bg-zinc-950 p-6">
      <div className="w-full max-w-3xl flex flex-col gap-5">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
            AutoDoc
          </h1>
          <p className="text-sm text-zinc-500 mt-1">
            Paste a public GitHub repository URL and get a generated README.md.
          </p>
        </div>

        <div className="flex gap-2">
          <input
            type="url"
            className="flex-1 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm text-zinc-900 shadow-sm outline-none focus:ring-2 focus:ring-zinc-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            placeholder="https://github.com/owner/repo"
            value={repoUrl}
            onChange={(e) => setRepoUrl(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && generate()}
            disabled={running}
          />
          <button
            onClick={generate}
            disabled={running || !repoUrl.trim()}
            className="rounded-xl bg-zinc-900 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-zinc-700 disabled:opacity-40 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            {running ? "Generating…" : "Generate"}
          </button>
        </div>

        {steps.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold uppercase tracking-widest text-zinc-400">
              Agent Steps
            </p>
            {steps.map((step, i) => (
              <div
                key={i}
                className={`rounded-lg border p-3 text-sm ${
                  step.type === "TOOL"
                    ? "border-blue-200 bg-blue-50 dark:border-blue-800 dark:bg-blue-950"
                    : step.type === "MEMORY"
                    ? "border-purple-200 bg-purple-50 dark:border-purple-800 dark:bg-purple-950"
                    : "border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900"
                }`}
              >
                <div className="flex items-center gap-2 mb-1 flex-wrap">
                  <span
                    className={`text-xs font-mono font-bold px-1.5 py-0.5 rounded ${
                      step.type === "TOOL"
                        ? "bg-blue-200 text-blue-800 dark:bg-blue-800 dark:text-blue-200"
                        : step.type === "MEMORY"
                        ? "bg-purple-200 text-purple-800 dark:bg-purple-800 dark:text-purple-200"
                        : "bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300"
                    }`}
                  >
                    {step.type}
                  </span>
                  <span className="font-medium text-zinc-700 dark:text-zinc-300">
                    {step.name}
                  </span>
                  {step.input && (
                    <span className="text-zinc-400 font-mono text-xs truncate max-w-sm">
                      {step.input}
                    </span>
                  )}
                </div>
                <p className="text-zinc-600 dark:text-zinc-400 text-xs whitespace-pre-wrap line-clamp-3">
                  {step.output}
                </p>
              </div>
            ))}
            {running && (
              <p className="text-xs text-zinc-400 animate-pulse">
                Agent is exploring the repository…
              </p>
            )}
          </div>
        )}

        {finalResult && finalResult.status === "DONE" && (
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold uppercase tracking-widest text-green-600 dark:text-green-400">
                README.md
              </p>
              <button
                onClick={copyReadme}
                className="text-xs rounded-lg border border-zinc-200 bg-white px-3 py-1.5 text-zinc-600 hover:bg-zinc-50 transition dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800"
              >
                {copied ? "Copied!" : "Copy"}
              </button>
            </div>
            <pre className="rounded-xl border border-zinc-200 bg-white p-5 text-xs text-zinc-800 whitespace-pre-wrap leading-relaxed overflow-auto max-h-[60vh] dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200">
              {finalResult.result}
            </pre>
          </div>
        )}

        {finalResult && finalResult.status === "FAILED" && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-5 dark:border-red-800 dark:bg-red-950">
            <p className="text-xs font-semibold uppercase tracking-widest text-red-600 dark:text-red-400 mb-2">
              Failed
            </p>
            <p className="text-sm text-red-800 dark:text-red-300">
              {finalResult.result}
            </p>
          </div>
        )}
      </div>
    </main>
  );
}
