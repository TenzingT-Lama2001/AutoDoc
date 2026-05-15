"use client";

import { useEffect, useState } from "react";

export default function Home() {
  const [status, setStatus] = useState<string>("checking...");
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  useEffect(() => {
    fetch(`${apiUrl}/health`)
      .then((r) => r.json())
      .then((data) => setStatus(data.status))
      .catch(() => setStatus("unreachable"));
  }, [apiUrl]);

  const ok = status === "ok";

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-zinc-50 dark:bg-zinc-950">
      <div className="rounded-2xl border border-zinc-200 bg-white p-10 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="mb-6 text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
          AutoDoc
        </h1>
        <div className="flex items-center gap-3">
          <span
            className={`h-3 w-3 rounded-full ${
              ok ? "bg-green-500" : status === "checking..." ? "bg-yellow-400 animate-pulse" : "bg-red-500"
            }`}
          />
          <span className="text-zinc-600 dark:text-zinc-400">
            Backend: <span className="font-mono font-medium text-zinc-900 dark:text-zinc-50">{status}</span>
          </span>
        </div>
      </div>
    </main>
  );
}
