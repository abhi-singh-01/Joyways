import { getApiBaseUrl } from "./config";

/** Default timeout for public API requests (ms). */
const DEFAULT_API_FETCH_TIMEOUT_MS = 10_000;

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
  const fieldErrors = json.errors
    ? Object.values(json.errors).filter((value) => typeof value === "string" && value.trim())
    : [];
  const withFields = fieldErrors.length > 0 ? `${base} ${fieldErrors.join(" ")}` : base;
  const requestId = json.requestId;
  if (requestId && status >= 500) {
    return `${withFields} (ref ${requestId})`;
  }
  return withFields;
}

/**
 * Combines an optional caller AbortSignal with a request timeout.
 * Caller abort and timeout abort both cancel the fetch.
 */
function createFetchAbortSignal(
  timeoutMs: number,
  callerSignal?: AbortSignal | null
): { signal: AbortSignal; clear: () => void } {
  const timeoutController = new AbortController();
  const timeoutId = setTimeout(() => timeoutController.abort(), timeoutMs);
  const clear = () => clearTimeout(timeoutId);

  if (!callerSignal) {
    return { signal: timeoutController.signal, clear };
  }

  if (typeof AbortSignal.any === "function") {
    return {
      signal: AbortSignal.any([callerSignal, timeoutController.signal]),
      clear,
    };
  }

  const merged = new AbortController();
  const abortFrom = (source: AbortSignal) => {
    if (!merged.signal.aborted) {
      merged.abort(source.reason);
    }
    clear();
  };

  if (callerSignal.aborted) {
    abortFrom(callerSignal);
    return { signal: merged.signal, clear };
  }

  callerSignal.addEventListener("abort", () => abortFrom(callerSignal), {
    once: true,
  });
  timeoutController.signal.addEventListener(
    "abort",
    () => abortFrom(timeoutController.signal),
    { once: true }
  );

  return { signal: merged.signal, clear };
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  const url = `${getApiBaseUrl()}${path}`;
  const requestId = newRequestId();
  const { signal, clear } = createFetchAbortSignal(
    DEFAULT_API_FETCH_TIMEOUT_MS,
    options?.signal
  );

  try {
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
        signal,
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
  } finally {
    clear();
  }
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
