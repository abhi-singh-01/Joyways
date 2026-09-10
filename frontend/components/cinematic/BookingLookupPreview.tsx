import { ArrowRight } from "lucide-react";
import { ButtonLink } from "@/components/ui/ButtonLink";

export function BookingLookupPreview() {
  return (
    <section className="bg-off-white py-20 sm:py-28">
      <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 sm:px-6 lg:grid-cols-2 lg:px-8">
        <div>
          <h2 className="font-display text-3xl font-semibold text-charcoal sm:text-5xl">
            Your journey stays with you.
          </h2>
          <p className="mt-4 max-w-md text-charcoal-muted">
            Look up a booking with the reference from your confirmation email. The panel
            beside this copy is illustrative — it is not a real booking.
          </p>
          <ButtonLink href="/bookings/lookup" className="mt-8">
            Find my booking
            <ArrowRight size={16} className="ml-2" />
          </ButtonLink>
        </div>

        <aside
          className="rounded-2xl border border-border bg-white p-6 shadow-sm sm:p-8"
          aria-label="Illustrative booking reference"
        >
          <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-charcoal-muted">
            Illustrative preview
          </p>
          <p className="mt-6 text-xs font-semibold uppercase tracking-[0.22em] text-forest">
            Your journey
          </p>
          <p className="mt-2 font-display text-3xl font-semibold text-charcoal">JW-XXXX-XXXXX</p>
          <dl className="mt-8 space-y-4 text-sm">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <dt className="text-charcoal-muted">Status</dt>
              <dd className="font-semibold uppercase tracking-wide text-forest">Confirmed</dd>
            </div>
            <div className="flex items-center justify-between border-b border-border pb-3">
              <dt className="text-charcoal-muted">Trip</dt>
              <dd className="text-charcoal">Your booked departure</dd>
            </div>
            <div className="flex items-center justify-between border-b border-border pb-3">
              <dt className="text-charcoal-muted">Date</dt>
              <dd className="text-charcoal">As on your confirmation</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-charcoal-muted">Travellers</dt>
              <dd className="text-charcoal">Named at checkout</dd>
            </div>
          </dl>
        </aside>
      </div>
    </section>
  );
}
