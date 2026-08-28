import { LoadingState } from "@/components/ui/LoadingState";

export default function TripsLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
      <LoadingState message="Loading trips..." fullPage />
    </div>
  );
}
