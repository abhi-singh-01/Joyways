const stages = [
  {
    title: "Trip",
    copy: "Open a live package: dates, itinerary, stay, and remaining seats.",
  },
  {
    title: "Dates",
    copy: "Each trip has a published departure window — you book that departure, not an open calendar.",
  },
  {
    title: "Seats",
    copy: "Choose how many travellers are joining this group departure.",
  },
  {
    title: "Traveller details",
    copy: "Enter names and contact details for the people on the booking.",
  },
  {
    title: "Secure payment",
    copy: "Pay with Razorpay. If checkout is interrupted, you can resume a pending payment from booking lookup.",
  },
  {
    title: "Confirmation",
    copy: "A booking reference is issued and confirmation is sent by email.",
  },
];

export function BookingExperience() {
  return (
    <section className="bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="max-w-3xl font-display text-3xl font-semibold text-charcoal sm:text-5xl">
          Book without the friction.
        </h2>
        <p className="mt-4 max-w-xl text-charcoal-muted">
          Guest checkout — no customer account. The sequence below matches the live booking flow.
        </p>

        <ol className="mt-14 overflow-hidden rounded-2xl border border-border bg-white">
          {stages.map((stage, index) => (
            <li
              key={stage.title}
              className="grid gap-3 border-b border-border px-5 py-6 last:border-b-0 sm:grid-cols-[7rem_1fr] sm:items-baseline sm:px-8"
            >
              <span className="font-display text-sm font-semibold text-forest">
                {String(index + 1).padStart(2, "0")} {stage.title}
              </span>
              <p className="text-sm leading-relaxed text-charcoal-muted">{stage.copy}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
