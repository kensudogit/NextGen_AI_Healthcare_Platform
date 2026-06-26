import { getAccessToken, oauthEnabled } from "@/lib/auth";

const API_BASE =
  typeof window === "undefined"
    ? process.env.INTERNAL_API_URL || "http://backend:8010"
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8010";

function authHeaders(init?: RequestInit): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init?.headers as Record<string, string>),
  };
  const token = typeof window !== "undefined" ? getAccessToken() : null;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: authHeaders(init),
    cache: "no-store",
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `API error ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export function apiUrl(path: string) {
  const base =
    typeof window === "undefined"
      ? process.env.INTERNAL_API_URL || "http://backend:8010"
      : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8010";
  const url = `${base}${path}`;
  if (oauthEnabled && typeof window !== "undefined") {
    const token = getAccessToken();
    if (token) {
      return `${url}?access_token=${encodeURIComponent(token)}`;
    }
  }
  return url;
}

export { oauthEnabled };
