import { adminFetch } from "./client";

export type AdminNotification = {
  id: number;
  type: string;
  title: string;
  message: string;
  relatedEntityType: string | null;
  relatedEntityId: number | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
};

export async function listAdminNotifications(): Promise<AdminNotification[]> {
  const response = await adminFetch<AdminNotification[]>("/api/admin/notifications", {
    cache: "no-store",
  });
  return response.data ?? [];
}

export async function getUnreadNotificationCount(): Promise<number> {
  const response = await adminFetch<{ count: number }>("/api/admin/notifications/unread-count", {
    cache: "no-store",
  });
  return response.data?.count ?? 0;
}

export async function markNotificationRead(id: number): Promise<void> {
  await adminFetch<void>(`/api/admin/notifications/${id}/read`, {
    method: "PATCH",
  });
}

export async function markAllNotificationsRead(): Promise<void> {
  await adminFetch<void>("/api/admin/notifications/read-all", {
    method: "PATCH",
  });
}
