"use client";

import { ErrorState } from "@/components/ui/ErrorState";

export default function SiteError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
      <ErrorState
        title="Something went wrong"
        message="We couldn't load this page. Please try again."
        onRetry={reset}
        fullPage
      />
    </div>
  );
}
