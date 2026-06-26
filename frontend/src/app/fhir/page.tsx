import { api } from "@/lib/api";

type Metadata = {
  resourceType: string;
  fhirVersion: string;
  status: string;
  rest?: Array<{ mode: string; resource?: Array<{ type: string; interaction?: Array<{ code: string }> }> }>;
};

const ENDPOINTS = [
  { method: "GET", path: "/fhir/R4/metadata", desc: "CapabilityStatement" },
  { method: "GET", path: "/fhir/R4/Patient", desc: "患者一覧" },
  { method: "GET", path: "/fhir/R4/Patient/{id}", desc: "患者詳細" },
  { method: "GET", path: "/fhir/R4/Appointment", desc: "予約一覧" },
  { method: "GET", path: "/fhir/R4/ImagingStudy", desc: "画像検査" },
  { method: "GET", path: "/fhir/R4/DiagnosticReport", desc: "診断レポート" },
];

export default async function FhirPage() {
  let meta: Metadata | null = null;
  let err = "";
  try {
    meta = await api<Metadata>("/fhir/R4/metadata");
  } catch (e) {
    err = e instanceof Error ? e.message : "Error";
  }

  const resources =
    meta?.rest?.[0]?.resource?.map((r) => ({
      type: r.type,
      interactions: r.interaction?.map((i) => i.code).join(", ") || "",
    })) || [];

  return (
    <>
      <h1 className="page-title">FHIR R4 API</h1>
      <p className="page-desc">他システム連携用 HL7 FHIR REST エンドポイント</p>
      {err && <p className="error">{err}</p>}

      {meta && (
        <div className="card" style={{ marginBottom: "1rem" }}>
          <h3>FHIR サーバー情報</h3>
          <p>
            Version: <strong>{meta.fhirVersion}</strong> · Status: {meta.status}
          </p>
        </div>
      )}

      <div className="card" style={{ marginBottom: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
          エンドポイント一覧
        </h3>
        <table>
          <thead>
            <tr>
              <th>Method</th>
              <th>Path</th>
              <th>説明</th>
              <th>Try</th>
            </tr>
          </thead>
          <tbody>
            {ENDPOINTS.map((ep) => (
              <tr key={ep.path}>
                <td>
                  <span className="badge info">{ep.method}</span>
                </td>
                <td className="mono">{ep.path}</td>
                <td>{ep.desc}</td>
                <td>
                  <a href={ep.path.replace("{id}", "1")} target="_blank" rel="noreferrer">
                    JSON ↗
                  </a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {resources.length > 0 && (
        <div className="card">
          <h3 style={{ marginBottom: "0.75rem", textTransform: "none", fontSize: "0.95rem", color: "var(--text)" }}>
            サポートリソース
          </h3>
          <table>
            <thead>
              <tr>
                <th>Resource</th>
                <th>Interactions</th>
              </tr>
            </thead>
            <tbody>
              {resources.map((r) => (
                <tr key={r.type}>
                  <td>{r.type}</td>
                  <td>{r.interactions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
