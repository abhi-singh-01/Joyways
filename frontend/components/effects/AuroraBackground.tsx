"use client";

import { cn } from "@/lib/utils";

type AuroraBackgroundProps = {
  className?: string;
};

export function AuroraBackground({ className }: AuroraBackgroundProps) {
  return (
    <div
      className={cn("pointer-events-none absolute inset-0 overflow-hidden", className)}
      aria-hidden
    >
      <div className="aurora-blob aurora-blob-1 absolute -left-1/4 top-0 h-[55%] w-[70%] rounded-full bg-forest-light/30 opacity-40 blur-3xl" />
      <div className="aurora-blob aurora-blob-2 absolute -right-1/4 top-1/4 h-[50%] w-[65%] rounded-full bg-[#40916c]/25 opacity-35 blur-3xl" />
      <div className="aurora-blob aurora-blob-3 absolute bottom-0 left-1/3 h-[45%] w-[55%] rounded-full bg-accent/15 opacity-30 blur-3xl" />
    </div>
  );
}
