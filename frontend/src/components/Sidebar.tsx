"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import LoginBar from "@/components/LoginBar";

const NAV = [
  { href: "/", label: "ダッシュボード" },
  { href: "/patients", label: "電子カルテ" },
  { href: "/appointments", label: "予約管理" },
  { href: "/imaging", label: "PACS / DICOM" },
  { href: "/radiology", label: "読影レポート" },
  { href: "/phone", label: "AI電話受付" },
  { href: "/hl7", label: "HL7 v2" },
  { href: "/fhir", label: "FHIR API" },
];

export default function Sidebar() {
  const path = usePathname();
  return (
    <aside className="sidebar">
      <h1>NextGen Healthcare</h1>
      <p className="sub">AI統合病院プラットフォーム</p>
      <nav>
        {NAV.map((n) => (
          <Link
            key={n.href}
            href={n.href}
            prefetch={false}
            className={`nav-link${path === n.href ? " active" : ""}`}
          >
            {n.label}
          </Link>
        ))}
      </nav>
      <LoginBar />
      <div style={{ marginTop: "1rem", fontSize: "0.75rem", color: "var(--muted)" }}>
        <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
          API Docs ↗
        </a>
      </div>
    </aside>
  );
}
