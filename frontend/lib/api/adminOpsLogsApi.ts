import { adminFetch } from "./client";

export type AdminPage<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminAuditLog = {
  id: number;
  actorName: string;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: number | null;
  summary: string;
  createdAt: string;
};

export type AdminEmailLog = {
  id: number;
  recipient: string;
  subject: string;
  template: string;
  status: string;
  attemptCount: number;
  sentAt: string | null;
  lastAttemptedAt: string | null;
  createdAt: string;
  failureReason: string | null;
};

export async function listAdminAuditLogs(page = 0): Promise<AdminPage<AdminAuditLog>> {
  const response = await adminFetch<AdminPage<AdminAuditLog>>(
    `/api/admin/audit-logs?page=${page}&size=50`,
    { cache: "no-store" }
  );
  return response.data ?? { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}

export async function listAdminEmailLogs(page = 0): Promise<AdminPage<AdminEmailLog>> {
  const response = await adminFetch<AdminPage<AdminEmailLog>>(
    `/api/admin/email-logs?page=${page}&size=50`,
    { cache: "no-store" }
  );
  return response.data ?? { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
}
