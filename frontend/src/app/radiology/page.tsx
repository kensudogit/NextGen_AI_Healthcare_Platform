"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";

type Report = {
  id: number;
  patient_id: number;
  patient_name: string;
  modality: string;
  report_text: string;
  ai_summary: string | null;
  ai_findings: string | null;
  urgency: string | null;
  radiologist: string | null;
  reported_at: string;
};

export default function RadiologyPage() {
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [summarizing, setSummarizing] = useState<number | null>(null);
  const [err, setErr] = useState("");

  const load = useCallback(async () => {
    try {
      const data = await api<{ reports: Report[] }>("/api/radiology/reports");
      setReports(data.reports);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function summarize(id: number) {
    setSummarizing(id);
    try {
      await api(`/api/radiology/reports/${id}/summarize`, { method: "POST" });
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally {
      setSummarizing(null);
    }
  }

  return (
    <>
      <h1 className="page-title">読影レポート · AI要約</h1>
      <p className="page-desc">放射線レポートの参照と AI による要約・所見抽出</p>
      {err && <p className="error">{err}</p>}
      {loading ? (
        <p>読み込み中…</p>
      ) : (
        reports.map((r) => (
          <div key={r.id} className="card" style={{ marginBottom: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.75rem" }}>
              <div>
                <strong>
                  {r.patient_name} — {r.modality}
                </strong>
                <div style={{ fontSize: "0.8rem", color: "var(--muted)" }}>
                  {r.radiologist} · {new Date(r.reported_at).toLocaleString("ja-JP")}
                </div>
              </div>
              {r.urgency && (
                <span className={`badge ${r.urgency === "high" ? "warn" : "ok"}`}>{r.urgency}</span>
              )}
            </div>

            <details style={{ marginBottom: "0.75rem" }}>
              <summary style={{ cursor: "pointer", fontSize: "0.875rem" }}>原文レポート</summary>
              <p className="mono" style={{ marginTop: "0.5rem", fontSize: "0.8rem" }}>
                {r.report_text}
              </p>
            </details>

            {r.ai_summary ? (
              <div style={{ background: "var(--surface2)", padding: "0.75rem", borderRadius: 8 }}>
                <div style={{ fontSize: "0.75rem", color: "var(--accent2)", marginBottom: "0.35rem" }}>AI 要約</div>
                <p style={{ fontSize: "0.875rem" }}>{r.ai_summary}</p>
                {r.ai_findings && (
                  <>
                    <div style={{ fontSize: "0.75rem", color: "var(--accent2)", margin: "0.5rem 0 0.35rem" }}>
                      主要所見
                    </div>
                    <p style={{ fontSize: "0.875rem" }}>{r.ai_findings}</p>
                  </>
                )}
              </div>
            ) : (
              <button className="btn" disabled={summarizing === r.id} onClick={() => summarize(r.id)}>
                {summarizing === r.id ? "要約中…" : "AI 要約を生成"}
              </button>
            )}
          </div>
        ))
      )}
    </>
  );
}
