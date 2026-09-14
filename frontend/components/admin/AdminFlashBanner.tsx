"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, X } from "lucide-react";
import { adminSuccessMessage } from "@/lib/adminSuccess";

export function AdminFlashBanner() {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const successKey = searchParams.get("success");
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const nextMessage = adminSuccessMessage(successKey);
    if (!nextMessage || !successKey) return;

    setMessage(nextMessage);

    const params = new URLSearchParams(searchParams.toString());
    params.delete("success");
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [successKey, searchParams, pathname, router]);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 6000);
    return () => window.clearTimeout(timer);
  }, [message]);

  if (!message) return null;

  return (
    <div
      role="status"
      className="mb-6 flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
    >
      <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-700" aria-hidden />
      <p className="flex-1 font-medium">{message}</p>
      <button
        type="button"
        onClick={() => setMessage(null)}
        className="rounded-md p-0.5 text-emerald-800/70 hover:bg-emerald-100 hover:text-emerald-950"
        aria-label="Dismiss success message"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
