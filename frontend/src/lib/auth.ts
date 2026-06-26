const KEYCLOAK_URL = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:8080";
const REALM = process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "nghealth";
const CLIENT_ID = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "nghealth-frontend";

export const oauthEnabled = process.env.NEXT_PUBLIC_OAUTH_ENABLED === "true";

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem("nghealth_access_token");
}

export function clearAccessToken() {
  sessionStorage.removeItem("nghealth_access_token");
}

export async function login(username: string, password: string): Promise<void> {
  const body = new URLSearchParams({
    grant_type: "password",
    client_id: CLIENT_ID,
    username,
    password,
  });
  const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) {
    throw new Error("Login failed");
  }
  const data = (await res.json()) as { access_token: string };
  sessionStorage.setItem("nghealth_access_token", data.access_token);
}

export function logout() {
  clearAccessToken();
}
