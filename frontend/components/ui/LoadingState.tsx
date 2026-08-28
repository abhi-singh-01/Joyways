import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

type LoadingStateProps = {
  message?: string;
  className?: string;
  fullPage?: boolean;
};

export function LoadingState({
  message = "Loading...",
  className,
  fullPage = false,
}: LoadingStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-4 text-charcoal-muted",
        fullPage ? "min-h-[50vh] py-20" : "py-12",
        className
      )}
      role="status"
      aria-live="polite"
    >
      <Loader2 className="h-8 w-8 animate-spin text-forest" aria-hidden />
      <p className="text-sm font-medium">{message}</p>
    </div>
  );
}
