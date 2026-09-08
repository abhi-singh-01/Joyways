import { ShieldCheck, MapPin, Bus, Search } from "lucide-react";

const highlights = [
  {
    icon: ShieldCheck,
    title: "Secure checkout",
    text: "Pay with Razorpay. We never store your card or UPI PIN.",
  },
  {
    icon: MapPin,
    title: "Delhi departures",
    text: "Group trips from Delhi and nearby pickup points.",
  },
  {
    icon: Bus,
    title: "Complete packages",
    text: "Transport, stay, and itinerary in one booking.",
  },
  {
    icon: Search,
    title: "Guest booking lookup",
    text: "Find your trip anytime with reference and email.",
  },
];

export function TrustStrip() {
  return (
    <section className="border-y border-border bg-white py-14 sm:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <p className="mb-8 text-center text-sm font-semibold uppercase tracking-[0.2em] text-charcoal-muted">
          Built for straightforward group travel
        </p>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {highlights.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.title}
                className="rounded-2xl border border-border bg-white px-6 py-8 text-center shadow-sm"
              >
                <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-forest/8 text-forest">
                  <Icon size={22} aria-hidden />
                </div>
                <p className="font-display text-lg font-semibold text-charcoal">{item.title}</p>
                <p className="mt-1 text-sm text-charcoal-muted">{item.text}</p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
