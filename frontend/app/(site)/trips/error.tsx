"use client";

import { ErrorState } from "@/components/ui/ErrorState";

export default function TripsError({
  error,
  reset,
}: Readonly<{
  error: Error & { digest?: string };
  reset: () => void;
}>) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
      <ErrorState
        title="Couldn't load trips"
        message={error.message || "We couldn't fetch the trip list. Please check your connection and try again."}
        onRetry={reset}
        fullPage
      />
    </div>
  );
}
