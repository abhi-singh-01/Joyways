"use client";

import { cn } from "@/lib/utils";

type MovingBorderProps = {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
  borderRadius?: string;
};

/** Aceternity-inspired animated gradient border wrapper */
export function MovingBorder({
  children,
  className,
  containerClassName,
  borderRadius = "9999px",
}: MovingBorderProps) {
  return (
    <div
      className={cn("relative p-[2px] overflow-hidden", containerClassName)}
      style={{ borderRadius }}
    >
      <div
        className="moving-border-gradient absolute inset-0 opacity-80"
        style={{ borderRadius }}
        aria-hidden
      />
      <div className={cn("relative bg-forest", className)} style={{ borderRadius }}>
        {children}
      </div>
    </div>
  );
}
