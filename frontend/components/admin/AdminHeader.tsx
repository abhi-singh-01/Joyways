"use client";

import { AdminNotificationBell } from "@/components/admin/AdminNotificationBell";

export function AdminHeader() {
  return (
    <header className="sticky top-0 z-40 flex h-16 items-center justify-between border-b border-border bg-white/95 px-6 backdrop-blur sm:px-8">
      <div>
        <p className="text-xs font-medium uppercase tracking-wider text-charcoal-muted">Admin</p>
        <p className="text-sm font-semibold text-charcoal">Dashboard</p>
      </div>
      <AdminNotificationBell />
    </header>
  );
}
