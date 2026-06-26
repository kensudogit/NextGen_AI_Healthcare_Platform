import Link from "next/link";
import { api } from "@/lib/api";

type Patient = {
  id: number;
  mrn: string;
  name: string;
  birth_date: string;
  gender: string;
  phone: string | null;
  allergies: string | null;
};

export default async function PatientsPage() {
  let patients: Patient[] = [];
  let err = "";
  try {
    const data = await api<{ patients: Patient[] }>("/api/emr/patients");
    patients = data.patients;
  } catch (e) {
    err = e instanceof Error ? e.message : "Error";
  }

  return (
    <>
      <h1 className="page-title">電子カルテ (EMR)</h1>
      <p className="page-desc">患者情報・診療記録の参照（FHIR Patient 連携）</p>
      {err && <p className="error">{err}</p>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>MRN</th>
              <th>氏名</th>
              <th>生年月日</th>
              <th>性別</th>
              <th>電話</th>
              <th>アレルギー</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {patients.map((p) => (
              <tr key={p.id}>
                <td className="mono">{p.mrn}</td>
                <td>{p.name}</td>
                <td>{p.birth_date}</td>
                <td>{p.gender}</td>
                <td>{p.phone || "—"}</td>
                <td>{p.allergies || "—"}</td>
                <td>
                  <Link href={`/patients/${p.id}`}>詳細</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
