"use client";

import { AdminNotificationBell } from "@/components/admin/AdminNotificationBell";
import { Menu } from "lucide-react";

type AdminHeaderProps = {
  onOpenMenu?: () => void;
};

export function AdminHeader({ onOpenMenu }: AdminHeaderProps) {
  return (
    <header className="sticky top-0 z-40 flex h-16 items-center justify-between border-b border-border bg-white/95 px-4 backdrop-blur sm:px-8">
      <div className="flex items-center gap-3">
        {onOpenMenu && (
          <button
            type="button"
            className="rounded-lg p-2 text-charcoal hover:bg-surface md:hidden"
            aria-label="Open navigation menu"
            onClick={onOpenMenu}
          >
            <Menu size={22} />
          </button>
        )}
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-charcoal-muted">Admin</p>
          <p className="text-sm font-semibold text-charcoal">Dashboard</p>
        </div>
      </div>
      <AdminNotificationBell />
    </header>
  );
}
