import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ADMIN_LOGIN_PATH = "/admin/login";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (!pathname.startsWith("/admin") || pathname.startsWith(ADMIN_LOGIN_PATH)) {
    return NextResponse.next();
  }

  // Server-side guard: when API is on the same site (reverse-proxied /api),
  // forward cookies to the backend auth check.
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "");
  if (!apiBaseUrl) {
    return NextResponse.next();
  }

  const cookieHeader = request.headers.get("cookie");
  if (!cookieHeader?.includes("joyway_admin_token=")) {
    return NextResponse.redirect(new URL(ADMIN_LOGIN_PATH, request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
