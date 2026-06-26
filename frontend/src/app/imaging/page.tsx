import { api, apiUrl } from "@/lib/api";

type Study = {
  id: number;
  patient_id: number;
  patient_name: string;
  study_uid: string;
  modality: string;
  body_part: string | null;
  description: string | null;
  instance_count: number;
  performed_at: string;
  has_preview: boolean;
};

export default async function ImagingPage() {
  let studies: Study[] = [];
  let err = "";
  try {
    const data = await api<{ studies: Study[] }>("/api/pacs/studies");
    studies = data.studies;
  } catch (e) {
    err = e instanceof Error ? e.message : "Error";
  }

  return (
    <>
      <h1 className="page-title">PACS / DICOM</h1>
      <p className="page-desc">画像検査の一覧・プレビュー参照（DICOM アップロード対応）</p>
      {err && <p className="error">{err}</p>}

      <div className="grid grid-2">
        {studies.map((s) => (
          <div key={s.id} className="card">
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem" }}>
              <strong>{s.modality}</strong>
              <span className="badge info">{s.body_part || "—"}</span>
            </div>
            <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
              患者: {s.patient_name} (ID {s.patient_id})
            </p>
            <p style={{ fontSize: "0.8rem", color: "var(--muted)", marginBottom: "0.75rem" }}>
              {s.description || "—"} · {new Date(s.performed_at).toLocaleString("ja-JP")}
            </p>
            {s.has_preview ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={apiUrl(`/api/pacs/studies/${s.id}/preview`)}
                alt={`${s.modality} preview`}
                className="preview-img"
                style={{ maxHeight: 240 }}
              />
            ) : (
              <p style={{ color: "var(--muted)", fontSize: "0.875rem" }}>プレビューなし</p>
            )}
            <div style={{ marginTop: "0.75rem", fontSize: "0.8rem" }}>
              <a href={apiUrl(`/api/pacs/studies/${s.id}/dicom`)}>DICOM ダウンロード</a>
              {" · "}
              <span className="mono">{s.study_uid.slice(0, 24)}…</span>
            </div>
          </div>
        ))}
      </div>

      {studies.length === 0 && !err && (
        <div className="card">
          <p>画像検査データがありません。シードデータまたは API でアップロードしてください。</p>
        </div>
      )}
    </>
  );
}
