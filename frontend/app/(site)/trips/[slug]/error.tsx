"use client";

import { ErrorState } from "@/components/ui/ErrorState";

export default function TripDetailError({
  error,
  reset,
}: Readonly<{
  error: Error & { digest?: string };
  reset: () => void;
}>) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
      <ErrorState
        title="Couldn't load trip"
        message={error.message || "We couldn't fetch this trip's details. Please try again."}
        onRetry={reset}
        fullPage
      />
    </div>
  );
}
