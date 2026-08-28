import { adminFetch, AdminAuthError } from "./client";

export type AdminProfile = {
  id: number;
  name: string;
  email: string;
  roles: string[];
};

export async function adminLogin(
  email: string,
  password: string
): Promise<AdminProfile> {
  const response = await adminFetch<AdminProfile>("/api/admin/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  if (!response.data) {
    throw new Error("Login failed");
  }
  return response.data;
}

export async function adminLogout(): Promise<void> {
  await adminFetch<void>("/api/admin/auth/logout", { method: "POST" });
}

export async function getAdminSession(): Promise<AdminProfile | null> {
  try {
    const response = await adminFetch<AdminProfile>("/api/admin/auth/me");
    return response.data ?? null;
  } catch (err) {
    if (err instanceof AdminAuthError) {
      return null;
    }
    throw err;
  }
}
