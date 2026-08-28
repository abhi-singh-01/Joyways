import Link from "next/link";
import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";

type EmptyStateProps = {
  title?: string;
  message?: string;
  actionLabel?: string;
  actionHref?: string;
  className?: string;
};

export function EmptyState({
  title = "No trips available",
  message = "Check back soon — new journeys from Delhi are on the way.",
  actionLabel = "Back to home",
  actionHref = "/",
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-4 px-4 py-20 text-center",
        className
      )}
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-surface">
        <MapPin className="h-8 w-8 text-forest/60" aria-hidden />
      </div>
      <div>
        <h2 className="font-display text-2xl font-semibold text-charcoal">{title}</h2>
        <p className="mt-2 max-w-md text-charcoal-muted">{message}</p>
      </div>
      {actionHref && (
        <Link href={actionHref}>
          <Button variant="outline">{actionLabel}</Button>
        </Link>
      )}
    </div>
  );
}
