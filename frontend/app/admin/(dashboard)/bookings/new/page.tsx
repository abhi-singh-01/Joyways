import Link from "next/link";
import { ManualBookingForm } from "@/components/admin/ManualBookingForm";

export default function NewAdminBookingPage() {
  return (
    <div>
      <Link href="/admin/bookings" className="text-sm font-medium text-forest hover:underline">
        ← Back to bookings
      </Link>
      <h1 className="mt-4 font-display text-3xl font-semibold text-charcoal">New booking</h1>
      <p className="mt-1 text-charcoal-muted">
        Create a booking for a phone, WhatsApp, or walk-in customer
      </p>
      <div className="mt-8">
        <ManualBookingForm />
      </div>
    </div>
  );
}
