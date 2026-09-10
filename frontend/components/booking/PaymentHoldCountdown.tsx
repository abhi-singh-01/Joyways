"use client";

import { useEffect, useState } from "react";

type PaymentHoldCountdownProps = {
  expiresAt: string | null | undefined;
  className?: string;
};

function remainingMs(expiresAt: string): number {
  return new Date(expiresAt).getTime() - Date.now();
}

function formatRemaining(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export function PaymentHoldCountdown({ expiresAt, className }: Readonly<PaymentHoldCountdownProps>) {
  const [msLeft, setMsLeft] = useState(() => (expiresAt ? remainingMs(expiresAt) : 0));

  useEffect(() => {
    if (!expiresAt) {
      return;
    }
    const tick = window.setInterval(() => setMsLeft(remainingMs(expiresAt)), 1000);
    return () => window.clearInterval(tick);
  }, [expiresAt]);

  if (!expiresAt) {
    return null;
  }

  if (msLeft <= 0) {
    return (
      <p className={className ?? "mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900"}>
        This payment hold has ended. Start a new booking if you still want seats.
      </p>
    );
  }

  return (
    <p className={className ?? "mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900"}>
      Complete payment in <span className="font-semibold tabular-nums">{formatRemaining(msLeft)}</span>{" "}
      to keep these seats.
    </p>
  );
}

export function isPaymentHoldExpired(
  expiresAt: string | null | undefined,
  nowMs: number = Date.now(),
): boolean {
  if (!expiresAt) {
    return false;
  }
  return new Date(expiresAt).getTime() - nowMs <= 0;
}
