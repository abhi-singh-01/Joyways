"use client";

import Link from "next/link";
import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function TripNotFound() {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center px-4 py-20 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-surface">
        <MapPin className="h-8 w-8 text-forest/60" />
      </div>
      <h1 className="mt-6 font-display text-3xl font-semibold text-charcoal">
        Trip not found
      </h1>
      <p className="mt-3 text-charcoal-muted">
        This trip may be unavailable or the link may be incorrect.
      </p>
      <Link href="/trips" className="mt-8">
        <Button>Browse all trips</Button>
      </Link>
    </div>
  );
}
