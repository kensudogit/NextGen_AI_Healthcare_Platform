"use client";

import { useEffect, useState } from "react";
import { login, logout, oauthEnabled, getAccessToken } from "@/lib/auth";

export default function LoginBar() {
  const [user, setUser] = useState("");
  const [pass, setPass] = useState("");
  const [err, setErr] = useState("");
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(!!getAccessToken());
  }, []);

  if (!oauthEnabled) return null;

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    try {
      await login(user, pass);
      setLoggedIn(true);
      window.location.reload();
    } catch {
      setErr("ログインに失敗しました (staff / staff123)");
    }
  }

  if (loggedIn) {
    return (
      <div style={{ marginTop: "1.5rem", fontSize: "0.8rem" }}>
        <span style={{ color: "var(--success)" }}>● OAuth2 認証済</span>
        <button
          className="btn secondary"
          style={{ marginLeft: "0.5rem", padding: "0.25rem 0.5rem", fontSize: "0.75rem" }}
          onClick={() => {
            logout();
            setLoggedIn(false);
            window.location.reload();
          }}
        >
          ログアウト
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleLogin} style={{ marginTop: "1.5rem" }}>
      <div className="label">OAuth2 (Keycloak)</div>
      <input className="input" placeholder="ユーザー" value={user} onChange={(e) => setUser(e.target.value)} />
      <input className="input" type="password" placeholder="パスワード" value={pass} onChange={(e) => setPass(e.target.value)} />
      <button className="btn" type="submit" style={{ width: "100%" }}>
        ログイン
      </button>
      {err && <p className="error">{err}</p>}
    </form>
  );
}
