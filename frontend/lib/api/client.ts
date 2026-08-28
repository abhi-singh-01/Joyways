import { getApiBaseUrl } from "./config";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  errors?: Record<string, string>;
  requestId?: string;
};

function newRequestId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `jw-${Date.now().toString(36)}`;
}

function errorMessage(json: ApiResponse<unknown>, status: number, fallback: string): string {
  const base = json.message || fallback;
  const requestId = json.requestId;
  if (requestId && status >= 500) {
    return `${base} (ref ${requestId})`;
  }
  return base;
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  const url = `${getApiBaseUrl()}${path}`;
  const requestId = newRequestId();
  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        "X-Request-ID": requestId,
        ...options?.headers,
      },
      next: options?.next ?? { revalidate: 60 },
    });
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }

  let json: ApiResponse<T>;
  try {
    json = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }

  if (!response.ok) {
    throw new Error(errorMessage(json, response.status, "Request failed"));
  }

  return json;
}

/** Admin API fetch — sends cookies, no caching */
export async function adminFetch<T>(
  path: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  const url = `${getApiBaseUrl()}${path}`;
  const requestId = newRequestId();
  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      credentials: "include",
      cache: "no-store",
      headers: {
        "X-Request-ID": requestId,
        ...(options?.body instanceof FormData
          ? {}
          : { "Content-Type": "application/json" }),
        ...options?.headers,
      },
    });
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }

  let json: ApiResponse<T>;
  try {
    json = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new Error("Unable to reach the server. Please try again.");
  }

  if (response.status === 401) {
    throw new AdminAuthError(json.message || "Authentication required.");
  }

  if (!response.ok) {
    throw new Error(errorMessage(json, response.status, "Request failed"));
  }

  return json;
}

export class AdminAuthError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "AdminAuthError";
  }
}
