"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Package,
  LogOut,
  Settings,
  ClipboardList,
  Bus,
  UserCircle,
  Route,
} from "lucide-react";
import { JoywayLogo } from "@/components/layout/JoywayLogo";
import { cn } from "@/lib/utils";
import { adminLogout } from "@/lib/api/authApi";
import { useRouter } from "next/navigation";

const navItems = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard, exact: true },
  { href: "/admin/packages", label: "Packages", icon: Package },
  { href: "/admin/bookings", label: "Bookings", icon: ClipboardList },
  { href: "/admin/trips", label: "Trips", icon: Route },
  { href: "/admin/vehicles", label: "Vehicles", icon: Bus },
  { href: "/admin/drivers", label: "Drivers", icon: UserCircle },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const router = useRouter();

  async function handleLogout() {
    try {
      await adminLogout();
    } finally {
      router.push("/admin/login");
      router.refresh();
    }
  }

  return (
    <aside className="flex h-full w-64 flex-col border-r border-border bg-charcoal text-white">
      <div className="border-b border-white/10 px-6 py-5">
        <JoywayLogo href="/admin" height={36} variant="onDark" layout="compact" />
        <p className="mt-0.5 text-xs text-white/50">Admin Panel</p>
      </div>

      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                active
                  ? "bg-forest text-white"
                  : "text-white/70 hover:bg-white/10 hover:text-white"
              )}
            >
              <Icon size={18} />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-white/10 px-3 py-4">
        <Link
          href="/"
          className="mb-1 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-white/50 hover:bg-white/10 hover:text-white"
        >
          <Settings size={18} />
          View Website
        </Link>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-white/70 hover:bg-white/10 hover:text-white"
        >
          <LogOut size={18} />
          Logout
        </button>
      </div>
    </aside>
  );
}
