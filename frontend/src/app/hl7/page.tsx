"use client";

import { FormEvent, useState } from "react";
import { api } from "@/lib/api";

export default function Hl7Page() {
  const [message, setMessage] = useState(
    "MSH|^~\\&|NGHEALTH|SEND|NGHEALTH|RECV|20260626120000||ADT^A01|1|P\nPID|1||MRN-10001^^^NG||田中^太郎"
  );
  const [result, setResult] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  async function parseMessage(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const data = await api<Record<string, unknown>>("/api/hl7/parse", {
        method: "POST",
        body: JSON.stringify({ message }),
      });
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
      const data = await api<{ message: string }>("/api/hl7/build", {
        method: "POST",
        body: JSON.stringify({
          type: "ADT",
          mrn: "MRN-10001",
          familyName: "田中",
          givenName: "太郎",
        }),
      });
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
