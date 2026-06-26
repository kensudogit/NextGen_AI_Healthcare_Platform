"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";

type Turn = { role: string; text: string };
type CallLog = {
  id: number;
  call_sid: string;
  intent: string | null;
  status: string;
  summary: string | null;
  started_at: string;
};

export default function PhonePage() {
  const [callSid, setCallSid] = useState<string | null>(null);
  const [turns, setTurns] = useState<Turn[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [logs, setLogs] = useState<CallLog[]>([]);
  const [err, setErr] = useState("");

  const loadLogs = useCallback(async () => {
    try {
      const data = await api<{ calls: CallLog[] }>("/api/phone/calls");
      setLogs(data.calls);
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  async function startCall() {
    setBusy(true);
    setErr("");
    try {
      const data = await api<{ call_sid: string; reply: string }>("/api/phone/start", {
        method: "POST",
        body: JSON.stringify({ caller_number: "090-0000-0000" }),
      });
      setCallSid(data.call_sid);
      setTurns([{ role: "assistant", text: data.reply }]);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally {
      setBusy(false);
    }
  }

  async function sendMessage(e: FormEvent) {
    e.preventDefault();
    if (!callSid || !input.trim()) return;
    const text = input.trim();
    setInput("");
    setTurns((t) => [...t, { role: "user", text }]);
    setBusy(true);
    try {
      const data = await api<{ reply: string; action: string }>("/api/phone/utterance", {
        method: "POST",
        body: JSON.stringify({ call_sid: callSid, text }),
      });
      setTurns((t) => [...t, { role: "assistant", text: data.reply }]);
      if (data.action === "transfer" || data.action === "end") {
        await endCall();
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally {
      setBusy(false);
    }
  }

  async function endCall() {
    if (!callSid) return;
    setBusy(true);
    try {
      const data = await api<{ summary: string }>("/api/phone/end", {
        method: "POST",
        body: JSON.stringify({ call_sid: callSid }),
      });
      setTurns((t) => [...t, { role: "assistant", text: `【通話終了】${data.summary}` }]);
      setCallSid(null);
      await loadLogs();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1 className="page-title">AI 電話受付</h1>
      <p className="page-desc">予約・問い合わせ対応シミュレータ（Amazon Connect / Twilio Webhook 互換 API）</p>
      {err && <p className="error">{err}</p>}

      <div className="grid grid-2">
        <div className="card">
          <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            通話シミュレーション
          </h3>
          {!callSid ? (
            <button className="btn" onClick={startCall} disabled={busy}>
              着信を開始
            </button>
          ) : (
            <>
              <div className="chat">
                {turns.map((t, i) => (
                  <div key={i} className={`bubble ${t.role}`}>
                    {t.text}
                  </div>
                ))}
              </div>
              <form onSubmit={sendMessage} style={{ display: "flex", gap: "0.5rem" }}>
                <input
                  className="input"
                  style={{ marginBottom: 0, flex: 1 }}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="例: 予約したいです / 診療時間を教えて"
                  disabled={busy}
                />
                <button className="btn" type="submit" disabled={busy}>
                  送信
                </button>
              </form>
              <button className="btn secondary" style={{ marginTop: "0.5rem" }} onClick={endCall} disabled={busy}>
                通話終了
              </button>
            </>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            通話ログ
          </h3>
          <table>
            <thead>
              <tr>
                <th>意図</th>
                <th>状態</th>
                <th>要約</th>
                <th>開始</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((c) => (
                <tr key={c.id}>
                  <td>{c.intent || "—"}</td>
                  <td>
                    <span className={`badge ${c.status === "completed" ? "ok" : "warn"}`}>{c.status}</span>
                  </td>
                  <td style={{ maxWidth: 200, overflow: "hidden", textOverflow: "ellipsis" }}>
                    {c.summary || "—"}
                  </td>
                  <td>{new Date(c.started_at).toLocaleString("ja-JP")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.5rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
          Webhook エンドポイント
        </h3>
        <ul style={{ fontSize: "0.875rem", paddingLeft: "1.25rem" }}>
          <li>
            <code>POST /api/phone/start</code> — 通話開始
          </li>
          <li>
            <code>POST /api/phone/utterance</code> — 発話処理
          </li>
          <li>
            <code>POST /api/phone/end</code> — 通話終了・要約
          </li>
        </ul>
      </div>
    </>
  );
}
