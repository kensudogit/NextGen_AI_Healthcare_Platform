import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

function publicOrigin(request: NextRequest): string {
  const host = (
    request.headers.get("x-forwarded-host") ||
    request.headers.get("host") ||
    request.nextUrl.host
  ).replace(/:8080$/i, "");
  const proto = request.headers.get("x-forwarded-proto") || "https";
  return `${proto}://${host}`;
}

export function middleware(request: NextRequest) {
  if (request.nextUrl.pathname === "/fhir/") {
    return NextResponse.redirect(`${publicOrigin(request)}/fhir`, 308);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/fhir/"],
};
