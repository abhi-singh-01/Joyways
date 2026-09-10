"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Bell, CheckCheck } from "lucide-react";
import {
  getUnreadNotificationCount,
  listAdminNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type AdminNotification,
} from "@/lib/api/adminNotificationsApi";
import { cn } from "@/lib/utils";

function notificationHref(notification: AdminNotification): string | null {
  if (notification.relatedEntityType === "BOOKING" && notification.relatedEntityId) {
    return `/admin/bookings/${notification.relatedEntityId}`;
  }
  return null;
}

export function AdminNotificationBell() {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const containerRef = useRef<HTMLDivElement>(null);

  const refresh = useCallback(async () => {
    try {
      const [items, count] = await Promise.all([
        listAdminNotifications(),
        getUnreadNotificationCount(),
      ]);
      setNotifications(items.slice(0, 20));
      setUnreadCount(count);
    } catch {
      // Silent fail for bell polling
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Deferred to a microtask so the first poll does not update state
    // synchronously during the effect, which would cascade a re-render.
    void Promise.resolve().then(refresh);
    const interval = setInterval(() => void refresh(), 60000);
    return () => clearInterval(interval);
  }, [refresh]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  async function handleOpen() {
    setOpen((prev) => !prev);
    if (!open) {
      await refresh();
    }
  }

  async function handleMarkRead(notification: AdminNotification) {
    if (!notification.read) {
      await markNotificationRead(notification.id);
      await refresh();
    }
    const href = notificationHref(notification);
    if (href) {
      setOpen(false);
    }
  }

  async function handleMarkAllRead() {
    await markAllNotificationsRead();
    await refresh();
  }

  function notificationList() {
    if (loading && notifications.length === 0) {
      return <p className="px-4 py-6 text-center text-sm text-charcoal-muted">Loading...</p>;
    }
    if (notifications.length === 0) {
      return <p className="px-4 py-6 text-center text-sm text-charcoal-muted">No notifications</p>;
    }
    return notifications.map((notification) => {
      const href = notificationHref(notification);
      const content = (
        <div
          className={cn(
            "border-b border-border px-4 py-3 text-left transition-colors hover:bg-muted/50",
            !notification.read && "bg-forest/5"
          )}
        >
          <p className="text-sm font-medium text-charcoal">{notification.title}</p>
          <p className="mt-0.5 text-xs text-charcoal-muted">{notification.message}</p>
          <p className="mt-1 text-[10px] uppercase tracking-wide text-charcoal-muted/70">
            {notification.type.replaceAll("_", " ")}
          </p>
        </div>
      );

      if (href) {
        return (
          <Link
            key={notification.id}
            href={href}
            onClick={() => void handleMarkRead(notification)}
          >
            {content}
          </Link>
        );
      }

      return (
        <button
          key={notification.id}
          type="button"
          className="block w-full"
          onClick={() => void handleMarkRead(notification)}
        >
          {content}
        </button>
      );
    });
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => void handleOpen()}
        className="relative rounded-xl p-2 text-charcoal-muted transition-colors hover:bg-muted hover:text-charcoal"
        aria-label="Notifications"
      >
        <Bell size={20} />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-forest px-1 text-[10px] font-semibold text-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-2xl border border-border bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <p className="text-sm font-semibold text-charcoal">Notifications</p>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={() => void handleMarkAllRead()}
                className="flex items-center gap-1 text-xs font-medium text-forest hover:underline"
              >
                <CheckCheck size={14} />
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {notificationList()}
          </div>
        </div>
      )}
    </div>
  );
}
