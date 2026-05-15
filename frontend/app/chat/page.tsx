"use client";

import { useRef, useState } from "react";

export default function ChatPage() {
  const [prompt, setPrompt] = useState("");
  const [response, setResponse] = useState("");
  const [streaming, setStreaming] = useState(false);
  const esRef = useRef<EventSource | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  function send() {
    if (!prompt.trim() || streaming) return;

    // Close any previous stream
    esRef.current?.close();
    setResponse("");
    setStreaming(true);

    const es = new EventSource(
      `${apiUrl}/chat/stream?prompt=${encodeURIComponent(prompt)}`
    );
    esRef.current = es;

    es.onmessage = (e) => {
      setResponse((prev) => prev + e.data);
    };

    es.onerror = () => {
      es.close();
      setStreaming(false);
    };
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) send();
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-zinc-50 dark:bg-zinc-950 p-6">
      <div className="w-full max-w-2xl flex flex-col gap-4">
        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
          AutoDoc Chat
        </h1>

        <textarea
          className="w-full rounded-xl border border-zinc-200 bg-white p-4 text-zinc-900 shadow-sm outline-none focus:ring-2 focus:ring-zinc-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50 resize-none"
          rows={4}
          placeholder="Ask anything… (Ctrl+Enter to send)"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={streaming}
        />

        <button
          onClick={send}
          disabled={streaming || !prompt.trim()}
          className="self-end rounded-xl bg-zinc-900 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-zinc-700 disabled:opacity-40 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          {streaming ? "Streaming…" : "Send"}
        </button>

        {response && (
          <div className="rounded-xl border border-zinc-200 bg-white p-5 text-zinc-800 shadow-sm whitespace-pre-wrap leading-relaxed dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200">
            {response}
            {streaming && (
              <span className="ml-1 inline-block h-4 w-0.5 animate-pulse bg-zinc-400" />
            )}
          </div>
        )}
      </div>
    </main>
  );
}
