import { cn } from "@/lib/utils";

const statusStyles: Record<string, string> = {
  DRAFT: "bg-amber-100 text-amber-900",
  PUBLISHED: "bg-emerald-100 text-emerald-800",
  PAUSED: "bg-slate-100 text-slate-700",
  COMPLETED: "bg-blue-100 text-blue-800",
  CANCELLED: "bg-red-100 text-red-800",
  AVAILABLE: "bg-emerald-100 text-emerald-800",
  ASSIGNED: "bg-blue-100 text-blue-800",
  ON_TRIP: "bg-indigo-100 text-indigo-800",
  MAINTENANCE: "bg-amber-100 text-amber-800",
  INACTIVE: "bg-gray-100 text-gray-700",
  PLANNED: "bg-slate-100 text-slate-700",
  READY: "bg-teal-100 text-teal-800",
  IN_PROGRESS: "bg-orange-100 text-orange-800",
  "LICENSE EXPIRED": "bg-red-100 text-red-800",
  "LICENSE EXPIRING SOON": "bg-amber-100 text-amber-800",
  VALID: "bg-emerald-100 text-emerald-800",
};

const statusLabels: Record<string, string> = {
  DRAFT: "Coming Soon",
};

export function StatusBadge({ status }: Readonly<{ status: string }>) {
  const label = statusLabels[status] ?? status.replaceAll("_", " ");
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
        statusStyles[status] ?? "bg-surface text-charcoal-muted"
      )}
    >
      {label}
    </span>
  );
}
