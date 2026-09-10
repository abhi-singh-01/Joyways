"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { formatPrice } from "@/lib/format";
import { Button } from "@/components/ui/Button";

type StickyBookBarProps = {
  pricePerPerson: number;
  bookable: boolean;
  waitlistable?: boolean;
  packageId: number;
  destination: string;
};

function stickyBookAction(
  bookable: boolean,
  waitlistable: boolean | undefined,
  packageId: number
) {
  if (bookable) {
    return (
      <Link href={`/book/${packageId}`} className="shrink-0">
        <Button size="md" className="min-w-[130px]">
          Book Now
        </Button>
      </Link>
    );
  }
  if (waitlistable) {
    return (
      <a href="#waitlist" className="shrink-0">
        <Button size="md" className="min-w-[130px]">
          Join waitlist
        </Button>
      </a>
    );
  }
  return (
    <Button size="md" disabled className="min-w-[130px]">
      Unavailable
    </Button>
  );
}

export function StickyBookBar({
  pricePerPerson,
  bookable,
  waitlistable,
  packageId,
  destination,
}: Readonly<StickyBookBarProps>) {
  return (
    <motion.div
      initial={{ y: 100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
      className="fixed bottom-0 left-0 right-0 z-40 border-t border-border bg-white/95 px-4 py-3 shadow-[0_-4px_24px_rgba(0,0,0,0.08)] backdrop-blur-md lg:hidden"
    >
      <div className="mx-auto flex max-w-lg items-center justify-between gap-4">
        <div>
          <p className="text-xs text-charcoal-muted">{destination}</p>
          <p className="text-lg font-semibold text-forest">
            {formatPrice(pricePerPerson)}
            <span className="text-xs font-normal text-charcoal-muted"> / person</span>
          </p>
        </div>
        {stickyBookAction(bookable, waitlistable, packageId)}
      </div>
    </motion.div>
  );
}
