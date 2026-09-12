import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ADMIN_LOGIN_PATH = "/admin/login";
const CANONICAL_HOST = "joyway.in";

export async function proxy(request: NextRequest) {
  const host = request.headers.get("host")?.split(":")[0]?.toLowerCase();
  if (host === "www.joyway.in") {
    const url = request.nextUrl.clone();
    url.hostname = CANONICAL_HOST;
    url.protocol = "https:";
    url.port = "";
    return NextResponse.redirect(url, 308);
  }

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
  // Literal string required: Next.js cannot statically analyze String.raw.
  // [.] is a literal dot so the matcher does not need escaped backslashes.
  matcher: [
    "/((?!_next/static|_next/image|.*[.](?:svg|png|jpg|jpeg|gif|webp|ico)$).*)",
  ],
};
