import { api } from "@/lib/api";

type Apt = {
  id: number;
  patient_name: string;
  department: string;
  purpose: string;
  status: string;
  scheduled_at: string;
  source: string;
};

export default async function AppointmentsPage() {
  let appointments: Apt[] = [];
  let err = "";
  try {
    const data = await api<{ appointments: Apt[] }>("/api/dashboard/appointments");
    appointments = data.appointments;
  } catch (e) {
    err = e instanceof Error ? e.message : "Error";
  }

  return (
    <>
      <h1 className="page-title">予約管理</h1>
      <p className="page-desc">職員登録・AI電話受付からの予約一覧</p>
      {err && <p className="error">{err}</p>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>患者名</th>
              <th>診療科</th>
              <th>目的</th>
              <th>日時</th>
              <th>状態</th>
              <th>経路</th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.patient_name}</td>
                <td>{a.department}</td>
                <td>{a.purpose}</td>
                <td>{new Date(a.scheduled_at).toLocaleString("ja-JP")}</td>
                <td>
                  <span className={`badge ${a.status === "booked" ? "ok" : "warn"}`}>{a.status}</span>
                </td>
                <td>
                  <span className={`badge ${a.source === "phone_ai" ? "info" : "ok"}`}>{a.source}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
