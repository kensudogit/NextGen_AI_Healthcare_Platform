import { api } from "@/lib/api";

type Stats = {
  hospital_name: string;
  counts: Record<string, number>;
  recent_appointments: Array<{
    id: number;
    patient_name: string;
    department: string;
    scheduled_at: string;
    source: string;
  }>;
  recent_calls: Array<{
    id: number;
    intent: string | null;
    status: string;
    summary: string;
    started_at: string;
  }>;
  integrations: Record<string, string>;
};

export default async function DashboardPage() {
  let stats: Stats | null = null;
  let err = "";
  try {
    stats = await api<Stats>("/api/dashboard/stats");
  } catch (e) {
    err = e instanceof Error ? e.message : "API接続エラー";
  }

  if (err) {
    return (
      <>
        <h1 className="page-title">ダッシュボード</h1>
        <p className="error">バックエンドに接続できません: {err}</p>
        <p className="page-desc">docker compose up または backend を port 8010 で起動してください。</p>
      </>
    );
  }

  const c = stats!.counts;
  const cards = [
    { label: "登録患者", value: c.patients },
    { label: "本日の予約", value: c.appointments_today },
    { label: "今後の予約", value: c.appointments_upcoming },
    { label: "画像検査", value: c.imaging_studies },
    { label: "読影レポート", value: c.radiology_reports },
    { label: "要約待ち", value: c.reports_pending_summary },
    { label: "本日の電話", value: c.phone_calls_today },
    { label: "通話中", value: c.active_phone_calls },
  ];

  return (
    <>
      <h1 className="page-title">{stats!.hospital_name}</h1>
      <p className="page-desc">職員向け統合ダッシュボード — リアルタイム概要</p>

      <div className="grid grid-4" style={{ marginBottom: "1.5rem" }}>
        {cards.map((card) => (
          <div key={card.label} className="card">
            <h3>{card.label}</h3>
            <div className="value">{card.value}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-2">
        <div className="card">
          <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            直近の予約
          </h3>
          <table>
            <thead>
              <tr>
                <th>患者</th>
                <th>診療科</th>
                <th>日時</th>
                <th>経路</th>
              </tr>
            </thead>
            <tbody>
              {stats!.recent_appointments.map((a) => (
                <tr key={a.id}>
                  <td>{a.patient_name}</td>
                  <td>{a.department}</td>
                  <td>{new Date(a.scheduled_at).toLocaleString("ja-JP")}</td>
                  <td>
                    <span className={`badge ${a.source === "phone_ai" ? "info" : "ok"}`}>{a.source}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            直近の電話受付
          </h3>
          <table>
            <thead>
              <tr>
                <th>意図</th>
                <th>状態</th>
                <th>要約</th>
              </tr>
            </thead>
            <tbody>
              {stats!.recent_calls.map((c) => (
                <tr key={c.id}>
                  <td>{c.intent || "—"}</td>
                  <td>
                    <span className={`badge ${c.status === "completed" ? "ok" : "warn"}`}>{c.status}</span>
                  </td>
                  <td>{c.summary || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.5rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
          システム連携
        </h3>
        <ul style={{ listStyle: "none", fontSize: "0.875rem" }}>
          {Object.entries(stats!.integrations).map(([k, v]) => (
            <li key={k} style={{ marginBottom: "0.35rem" }}>
              <strong>{k}</strong>:{" "}
              {k === "fhir_base" ? (
                <a href="/fhir" className="mono">
                  {v}
                </a>
              ) : (
                <code className="mono">{v}</code>
              )}
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}
