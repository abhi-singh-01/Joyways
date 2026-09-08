import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ADMIN_LOGIN_PATH = "/admin/login";

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (!pathname.startsWith("/admin") || pathname.startsWith(ADMIN_LOGIN_PATH)) {
    return NextResponse.next();
  }

  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "");
  if (!apiBaseUrl) {
    return NextResponse.redirect(new URL(ADMIN_LOGIN_PATH, request.url));
  }

  const cookieHeader = request.headers.get("cookie");
  if (!cookieHeader?.includes("joyway_admin_token=")) {
    return NextResponse.redirect(new URL(ADMIN_LOGIN_PATH, request.url));
  }

  try {
    const sessionResponse = await fetch(`${apiBaseUrl}/api/admin/auth/me`, {
      headers: { cookie: cookieHeader },
      cache: "no-store",
    });

    if (!sessionResponse.ok) {
      return NextResponse.redirect(new URL(ADMIN_LOGIN_PATH, request.url));
    }
  } catch {
    return NextResponse.redirect(new URL(ADMIN_LOGIN_PATH, request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
