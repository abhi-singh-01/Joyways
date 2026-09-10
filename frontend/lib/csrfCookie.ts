const CSRF_COOKIE = /(?:^|;\s*)XSRF-TOKEN=([^;]*)/;

/** Read the XSRF-TOKEN value from a Cookie header or document.cookie string. */
export function readCsrfTokenFromCookie(cookie: string): string | null {
  const match = CSRF_COOKIE.exec(cookie);
  if (!match) {
    return null;
  }
  return decodeURIComponent(match[1]);
}
