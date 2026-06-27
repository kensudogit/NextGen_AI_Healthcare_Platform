import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
  if (request.nextUrl.pathname === "/fhir/") {
    return NextResponse.redirect(new URL("/fhir", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/fhir/"],
};
