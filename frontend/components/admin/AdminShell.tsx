"use client";

import { useCallback, useEffect, useState } from "react";
import { AdminHeader } from "@/components/admin/AdminHeader";
import { AdminSidebar } from "@/components/admin/AdminSidebar";
import { cn } from "@/lib/utils";

type AdminShellProps = {
  children: React.ReactNode;
};

export function AdminShell({ children }: Readonly<AdminShellProps>) {
  const [mobileOpen, setMobileOpen] = useState(false);

  const closeMobile = useCallback(() => setMobileOpen(false), []);

  useEffect(() => {
    if (!mobileOpen) {
      return;
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closeMobile();
      }
    }

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeyDown);
    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [mobileOpen, closeMobile]);

  return (
    <div className="flex min-h-screen">
      <div className="fixed inset-y-0 hidden w-64 md:block">
        <AdminSidebar />
      </div>

      <div
        className={cn(
          "fixed inset-0 z-50 md:hidden",
          mobileOpen ? "pointer-events-auto" : "pointer-events-none"
        )}
        aria-hidden={!mobileOpen}
      >
        <button
          type="button"
          className={cn(
            "absolute inset-0 bg-charcoal/50 transition-opacity",
            mobileOpen ? "opacity-100" : "opacity-0"
          )}
          aria-label="Close navigation menu"
          onClick={closeMobile}
        />
        <dialog
          open={mobileOpen}
          className={cn(
            "absolute inset-y-0 left-0 m-0 h-full w-64 max-w-[85vw] transform border-0 bg-transparent p-0 transition-transform duration-200",
            mobileOpen ? "translate-x-0" : "-translate-x-full"
          )}
          aria-label="Admin navigation"
          onCancel={(event) => {
            event.preventDefault();
            closeMobile();
          }}
        >
          <AdminSidebar onNavigate={closeMobile} />
        </dialog>
      </div>

      <div className="flex flex-1 flex-col md:ml-64">
        <AdminHeader onOpenMenu={() => setMobileOpen(true)} />
        <main className="flex-1 p-6 sm:p-8">{children}</main>
      </div>
    </div>
  );
}
