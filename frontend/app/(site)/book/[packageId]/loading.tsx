import { LoadingState } from "@/components/ui/LoadingState";

export default function BookLoading() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20">
      <LoadingState message="Loading booking..." fullPage />
    </div>
  );
}
