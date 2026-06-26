"use client";

import { FormEvent, useState } from "react";

export default function Hl7Page() {
  const [message, setMessage] = useState(
    "MSH|^~\\&|NGHEALTH|SEND|NGHEALTH|RECV|20260626120000||ADT^A01|1|P\rPID|1||MRN-10001^^^NG||田中^太郎"
  );
  const [result, setResult] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  async function parseMessage(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const res = await fetch("/api/hl7/parse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message }),
      });
      const text = await res.text();
      if (!res.ok) {
        throw new Error(text || `HTTP ${res.status}`);
      }
      const data = JSON.parse(text) as Record<string, unknown>;
      setResult(JSON.stringify(data, null, 2));
    } catch (ex) {
      setErr(ex instanceof Error ? ex.message : "Error");
    } finally {
      setBusy(false);
    }
  }

  async function buildAdt() {
    setBusy(true);
    setErr("");
    try {
      const res = await fetch("/api/hl7/build", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type: "ADT",
          mrn: "MRN-10001",
          familyName: "田中",
          givenName: "太郎",
        }),
      });
      const text = await res.text();
      if (!res.ok) {
        throw new Error(text || `HTTP ${res.status}`);
      }
      const data = JSON.parse(text) as { message: string };
      setMessage(data.message);
      setResult("ADT^A01 メッセージを生成しました");
    } catch (ex) {
      setErr(ex instanceof Error ? ex.message : "Error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1 className="page-title">HL7 v2 連携</h1>
      <p className="page-desc">ADT / ORU メッセージのパース・生成（HAPI HL7）</p>
      {err && <p className="error">{err}</p>}

      <div className="card" style={{ marginBottom: "1rem" }}>
        <label className="label">HL7 メッセージ (pipe-delimited)</label>
        <textarea className="textarea" value={message} onChange={(e) => setMessage(e.target.value)} rows={8} />
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className="btn" onClick={parseMessage} disabled={busy}>
            パース
          </button>
          <button className="btn secondary" onClick={buildAdt} disabled={busy}>
            ADT^A01 生成
          </button>
        </div>
      </div>

      {result && (
        <div className="card">
          <h3 style={{ marginBottom: "0.5rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            結果
          </h3>
          <pre className="mono">{result}</pre>
        </div>
      )}
    </>
  );
}
