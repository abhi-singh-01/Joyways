"use client";

import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";

type ErrorStateProps = {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
  fullPage?: boolean;
};

export function ErrorState({
  title = "Something went wrong",
  message = "Something went wrong. Please try again.",
  onRetry,
  className,
  fullPage = false,
}: ErrorStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-4 px-4 text-center",
        fullPage ? "min-h-[50vh] py-20" : "py-12",
        className
      )}
      role="alert"
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-surface">
        <AlertCircle className="h-7 w-7 text-forest" aria-hidden />
      </div>
      <div>
        <h2 className="font-display text-xl font-semibold text-charcoal">{title}</h2>
        <p className="mt-2 max-w-md text-sm text-charcoal-muted">{message}</p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw size={16} className="mr-2" />
          Try again
        </Button>
      )}
    </div>
  );
}
