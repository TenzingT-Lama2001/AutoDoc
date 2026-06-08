"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";

interface StepRecord {
  stepOrder: number;
  type: "LLM" | "TOOL" | "MEMORY" | "THOUGHT";
  name: string;
  input: string | null;
  output: string | null;
  timestamp: string;
}

interface RunDetail {
  id: string;
  repoUrl: string;
  strategy: string;
  status: "RUNNING" | "DONE" | "FAILED";
  startedAt: string;
  completedAt: string | null;
  durationSeconds: number;
  result: string | null;
  steps: StepRecord[];
}

const STEP_CARD: Record<string, string> = {
  TOOL: "border-blue-200 bg-blue-50 dark:border-blue-800 dark:bg-blue-950",
  MEMORY: "border-purple-200 bg-purple-50 dark:border-purple-800 dark:bg-purple-950",
  THOUGHT: "border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-950",
  LLM: "border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900",
};

const STEP_BADGE: Record<string, string> = {
  TOOL: "bg-blue-200 text-blue-800 dark:bg-blue-800 dark:text-blue-200",
  MEMORY: "bg-purple-200 text-purple-800 dark:bg-purple-800 dark:text-purple-200",
  THOUGHT: "bg-amber-200 text-amber-800 dark:bg-amber-800 dark:text-amber-200",
  LLM: "bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300",
};

const STATUS_STYLES: Record<string, string> = {
  DONE: "text-green-600 dark:text-green-400",
  FAILED: "text-red-600 dark:text-red-400",
  RUNNING: "text-amber-600 dark:text-amber-400",
};

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}m ${s}s`;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString();
}

function shortRepo(url: string): string {
  return url.replace(/^https?:\/\/github\.com\//, "");
}

function StepCard({ step }: { step: StepRecord }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div
      className={`rounded-lg border p-3 text-sm cursor-pointer select-none ${STEP_CARD[step.type] ?? STEP_CARD.LLM}`}
      onClick={() => setExpanded((v) => !v)}
    >
      <div className="flex items-center gap-2 flex-wrap">
        <span className={`text-xs font-mono font-bold px-1.5 py-0.5 rounded ${STEP_BADGE[step.type] ?? STEP_BADGE.LLM}`}>
          {step.type}
        </span>
        <span className="font-medium text-zinc-700 dark:text-zinc-300">{step.name}</span>
        {step.input && (
          <span className="text-zinc-400 font-mono text-xs truncate max-w-xs">{step.input}</span>
        )}
        <span className="ml-auto text-xs text-zinc-400 font-mono">{formatTime(step.timestamp)}</span>
        <span className="text-xs text-zinc-400">{expanded ? "▲" : "▼"}</span>
      </div>

      {step.output && !expanded && (
        <p className="mt-1.5 text-zinc-600 dark:text-zinc-400 text-xs whitespace-pre-wrap line-clamp-2">
          {step.output}
        </p>
      )}

      {expanded && (
        <div className="mt-2 flex flex-col gap-2">
          {step.input && (
            <div>
              <p className="text-xs font-semibold text-zinc-400 uppercase tracking-wide mb-1">Input</p>
              <pre className="text-xs text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap bg-white/60 dark:bg-black/20 rounded p-2 overflow-auto max-h-48">
                {step.input}
              </pre>
            </div>
          )}
          {step.output && (
            <div>
              <p className="text-xs font-semibold text-zinc-400 uppercase tracking-wide mb-1">Output</p>
              <pre className="text-xs text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap bg-white/60 dark:bg-black/20 rounded p-2 overflow-auto max-h-64">
                {step.output}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function RunDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [run, setRun] = useState<RunDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  useEffect(() => {
    fetch(`${apiUrl}/runs/${id}`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then(setRun)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id, apiUrl]);

  function copyReadme() {
    if (!run?.result) return;
    navigator.clipboard.writeText(run.result).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  return (
    <main className="flex min-h-screen flex-col items-center bg-zinc-50 dark:bg-zinc-950 p-6">
      <div className="w-full max-w-3xl flex flex-col gap-5">
        <Link
          href="/runs"
          className="text-sm text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300 transition w-fit"
        >
          ← Back to Runs
        </Link>

        {loading && (
          <p className="text-sm text-zinc-400 animate-pulse">Loading run…</p>
        )}

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-950">
            <p className="text-sm text-red-700 dark:text-red-300">
              Failed to load run: {error}
            </p>
          </div>
        )}

        {run && (
          <>
            <div className="rounded-xl border border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900 p-5 flex flex-col gap-1">
              <p className="text-base font-semibold text-zinc-800 dark:text-zinc-200 font-mono">
                {shortRepo(run.repoUrl)}
              </p>
              <div className="flex items-center gap-3 flex-wrap text-xs text-zinc-500">
                {run.strategy && <span>{run.strategy}</span>}
                <span className={`font-medium ${STATUS_STYLES[run.status] ?? ""}`}>
                  {run.status}
                </span>
                <span>{run.steps.length} steps</span>
                <span>{formatDuration(run.durationSeconds)}</span>
                <span>{new Date(run.startedAt).toLocaleString()}</span>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold uppercase tracking-widest text-zinc-400">
                Trace — {run.steps.length} steps
              </p>
              {run.steps.map((step) => (
                <StepCard key={step.stepOrder} step={step} />
              ))}
            </div>

            {run.result && run.status === "DONE" && (
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
                  {run.result}
                </pre>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}
