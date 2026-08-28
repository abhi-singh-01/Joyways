import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/**
 * Lightweight frontend liveness probe for container and Azure availability tests.
 * Does not call the backend or expose configuration.
 */
export function GET() {
  return NextResponse.json({ status: "UP" });
}
