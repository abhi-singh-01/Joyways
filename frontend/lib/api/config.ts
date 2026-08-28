/**
 * Resolves the public backend API base URL.
 * In production builds, NEXT_PUBLIC_API_BASE_URL must be set at build time.
 */
export function getApiBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  if (configured) {
    return configured.replace(/\/$/, "");
  }
  if (process.env.NODE_ENV === "production") {
    throw new Error(
      "NEXT_PUBLIC_API_BASE_URL is required in production. Set it before running next build."
    );
  }
  return "http://localhost:8080";
}
