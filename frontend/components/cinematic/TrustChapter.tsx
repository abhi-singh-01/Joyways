const capabilities = [
  {
    title: "Secure payments",
    copy: "Verified Razorpay checkout. JOYWAY does not store card numbers or UPI PINs.",
  },
  {
    title: "Confirmed bookings",
    copy: "Seats and payment are processed on the server before a booking is marked confirmed.",
  },
  {
    title: "Booking lookup",
    copy: "Open your booking anytime with the reference and the email used at checkout.",
  },
  {
    title: "Email confirmation",
    copy: "Booking and payment messages are sent to the email on the booking.",
  },
  {
    title: "Real support",
    copy: "Contact JOYWAY when you need help with a trip or an existing booking.",
  },
];

export function TrustChapter() {
  return (
    <section className="bg-forest-dark py-20 text-white sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="max-w-3xl font-display text-3xl font-semibold sm:text-5xl">
          Travel with confidence.
        </h2>
        <p className="mt-4 max-w-xl text-white/70">
          What the product actually does — not visitor counts or star ratings.
        </p>

        <ul className="mt-14 divide-y divide-white/15 border-y border-white/15">
          {capabilities.map((item) => (
            <li
              key={item.title}
              className="grid gap-2 py-8 sm:grid-cols-[minmax(12rem,18rem)_1fr] sm:items-baseline"
            >
              <h3 className="font-display text-xl font-semibold">{item.title}</h3>
              <p className="text-sm leading-relaxed text-white/70">{item.copy}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
