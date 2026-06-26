import Link from "next/link";
import { api } from "@/lib/api";

type Detail = {
  patient: {
    id: number;
    mrn: string;
    name: string;
    birth_date: string;
    gender: string;
    phone: string | null;
    email: string | null;
    allergies: string | null;
    fhir_url: string;
  };
  encounters: Array<{
    id: number;
    type: string;
    chief_complaint: string | null;
    diagnosis: string | null;
    provider: string | null;
    started_at: string;
  }>;
  notes: Array<{
    id: number;
    note_type: string;
    author: string;
    content: string;
    created_at: string;
  }>;
};

export default async function PatientDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let detail: Detail | null = null;
  let err = "";
  try {
    detail = await api<Detail>(`/api/emr/patients/${id}`);
  } catch (e) {
    err = e instanceof Error ? e.message : "Error";
  }

  if (err || !detail) {
    return (
      <>
        <Link href="/patients">← 一覧</Link>
        <p className="error">{err || "Not found"}</p>
      </>
    );
  }

  const p = detail.patient;
  return (
    <>
      <Link href="/patients">← 患者一覧</Link>
      <h1 className="page-title" style={{ marginTop: "0.75rem" }}>
        {p.name}
      </h1>
      <p className="page-desc">
        MRN: {p.mrn} · FHIR: <a href={`http://localhost:8010${p.fhir_url}`}>{p.fhir_url}</a>
      </p>

      <div className="grid grid-2">
        <div className="card">
          <h3>基本情報</h3>
          <p>生年月日: {p.birth_date}</p>
          <p>性別: {p.gender}</p>
          <p>電話: {p.phone || "—"}</p>
          <p>メール: {p.email || "—"}</p>
          <p>アレルギー: {p.allergies || "なし"}</p>
        </div>
        <div className="card">
          <h3>診療 (Encounter)</h3>
          {detail.encounters.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>記録なし</p>
          ) : (
            detail.encounters.map((e) => (
              <div key={e.id} style={{ marginBottom: "0.75rem", fontSize: "0.875rem" }}>
                <strong>{e.type}</strong> — {e.provider}
                <br />
                主訴: {e.chief_complaint || "—"}
                <br />
                診断: {e.diagnosis || "—"}
              </div>
            ))
          )}
        </div>
      </div>

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
          カルテ記載
        </h3>
        {detail.notes.map((n) => (
          <div key={n.id} style={{ marginBottom: "1rem", paddingBottom: "1rem", borderBottom: "1px solid var(--border)" }}>
            <div style={{ fontSize: "0.75rem", color: "var(--muted)", marginBottom: "0.25rem" }}>
              {n.note_type} · {n.author} · {new Date(n.created_at).toLocaleString("ja-JP")}
            </div>
            <p style={{ fontSize: "0.875rem", whiteSpace: "pre-wrap" }}>{n.content}</p>
          </div>
        ))}
      </div>
    </>
  );
}
