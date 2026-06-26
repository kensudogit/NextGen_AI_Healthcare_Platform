"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

async function parseResponse(res: Response) {
  const text = await res.text();
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text) as Record<string, unknown>;
  } catch {
    throw new Error(text);
  }
}

export function PacsExportImportForm() {
  const router = useRouter();
  const [path, setPath] = useState("/app/pacs-export");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleImport(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setStatus("");
    try {
      const res = await fetch("/api/pacs/import/export-folder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ path }),
      });
      const data = await parseResponse(res);
      if (!res.ok) {
        throw new Error(String(data.message || data.error || `HTTP ${res.status}`));
      }
      setStatus(
        `取り込み完了: ${data.imported} 検査 (${data.skipped_existing} 件スキップ)`
      );
      router.refresh();
    } catch (err) {
      setStatus(err instanceof Error ? err.message : "取り込みに失敗しました");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleImport} className="card" style={{ marginBottom: "1.25rem" }}>
      <h2 style={{ fontSize: "1rem", marginBottom: "0.5rem" }}>PACS エクスポート取り込み</h2>
      <p style={{ fontSize: "0.875rem", color: "var(--muted)", marginBottom: "0.75rem" }}>
        PATIENT_00000001 / STUDY_... / SERIES_... / IMG000001.dcm 形式のフォルダを読み込みます
      </p>
      <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
        <input
          type="text"
          value={path}
          onChange={(e) => setPath(e.target.value)}
          placeholder="/app/pacs-export"
          style={{ flex: "1 1 240px", minWidth: 0 }}
        />
        <button type="submit" disabled={loading}>
          {loading ? "取り込み中…" : "フォルダ取り込み"}
        </button>
      </div>
      {status && (
        <p style={{ marginTop: "0.75rem", fontSize: "0.875rem" }} className={status.includes("完了") ? "" : "error"}>
          {status}
        </p>
      )}
    </form>
  );
}
