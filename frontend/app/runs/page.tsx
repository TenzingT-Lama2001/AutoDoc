"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

interface RunSummary {
  id: string;
  repoUrl: string;
  strategy: string;
  status: "RUNNING" | "DONE" | "FAILED";
  startedAt: string;
  completedAt: string | null;
  durationSeconds: number;
  stepCount: number;
  inputTokens: number;
  outputTokens: number;
}

const INPUT_RATE = 1.00;
const OUTPUT_RATE = 5.00;

function computeCost(inputTokens: number, outputTokens: number): number {
  return (inputTokens / 1_000_000) * INPUT_RATE + (outputTokens / 1_000_000) * OUTPUT_RATE;
}

function formatCost(cost: number): string {
  return `$${cost.toFixed(4)}`;
}

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}m ${s}s`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

function shortRepo(url: string): string {
  return url.replace(/^https?:\/\/github\.com\//, "");
}

const STATUS_STYLES: Record<string, string> = {
  DONE: "bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300",
  FAILED: "bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300",
  RUNNING: "bg-amber-100 text-amber-700 dark:bg-amber-900 dark:text-amber-300",
};

export default function RunsPage() {
  const [runs, setRuns] = useState<RunSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  useEffect(() => {
    fetch(`${apiUrl}/runs`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then(setRuns)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [apiUrl]);

  const doneRuns = runs.filter((r) => r.status === "DONE");
  const avgSteps =
    doneRuns.length > 0
      ? Math.round(doneRuns.reduce((s, r) => s + r.stepCount, 0) / doneRuns.length)
      : 0;
  const totalCost = doneRuns.reduce(
    (s, r) => s + computeCost(r.inputTokens, r.outputTokens),
    0
  );

  return (
    <main className="flex min-h-screen flex-col items-center bg-zinc-50 dark:bg-zinc-950 p-6">
      <div className="w-full max-w-4xl flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
              Past Runs
            </h1>
            <p className="text-sm text-zinc-500 mt-1">
              Every agent run, stored and inspectable.
            </p>
          </div>
          <Link
            href="/autodoc"
            className="rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-zinc-700 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            + New Run
          </Link>
        </div>

        {loading && (
          <p className="text-sm text-zinc-400 animate-pulse">Loading runs…</p>
        )}

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-950">
            <p className="text-sm text-red-700 dark:text-red-300">
              Failed to load runs: {error}
            </p>
          </div>
        )}

        {!loading && !error && runs.length === 0 && (
          <p className="text-sm text-zinc-500">
            No runs yet.{" "}
            <Link href="/autodoc" className="underline underline-offset-2">
              Generate your first README
            </Link>
            .
          </p>
        )}

        {runs.length > 0 && (
          <>
            <div className="flex items-center gap-6 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-5 py-3 text-xs text-zinc-500">
              <span>
                <span className="font-semibold text-zinc-700 dark:text-zinc-300">{runs.length}</span> runs
              </span>
              <span className="text-zinc-200 dark:text-zinc-700">|</span>
              <span>
                avg{" "}
                <span className="font-semibold text-zinc-700 dark:text-zinc-300">{avgSteps}</span> steps
              </span>
              <span className="text-zinc-200 dark:text-zinc-700">|</span>
              <span>
                ~<span className="font-semibold text-zinc-700 dark:text-zinc-300">{formatCost(totalCost)}</span> total
              </span>
            </div>

            <div className="flex flex-col divide-y divide-zinc-100 dark:divide-zinc-800 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 overflow-hidden">
              {runs.map((run) => (
                <Link
                  key={run.id}
                  href={`/runs/${run.id}`}
                  className="flex items-center gap-4 px-5 py-4 hover:bg-zinc-50 dark:hover:bg-zinc-800 transition group"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-zinc-800 dark:text-zinc-200 truncate font-mono">
                      {shortRepo(run.repoUrl)}
                    </p>
                    <p className="text-xs text-zinc-400 mt-0.5">
                      {formatDate(run.startedAt)}
                    </p>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    {run.strategy && (
                      <span className="text-xs text-zinc-400">{run.strategy}</span>
                    )}
                    <span className="text-xs text-zinc-400">
                      {run.stepCount} steps
                    </span>
                    <span className="text-xs text-zinc-400">
                      {formatDuration(run.durationSeconds)}
                    </span>
                    {run.inputTokens > 0 && (
                      <span className="text-xs text-zinc-400 font-mono">
                        {formatCost(computeCost(run.inputTokens, run.outputTokens))}
                      </span>
                    )}
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_STYLES[run.status] ?? ""}`}
                    >
                      {run.status}
                    </span>
                    <span className="text-zinc-300 dark:text-zinc-600 group-hover:text-zinc-500 dark:group-hover:text-zinc-400 transition">
                      →
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </div>
    </main>
  );
}
