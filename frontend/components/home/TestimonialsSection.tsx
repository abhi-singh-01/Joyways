import { Quote } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

const promises = [
  {
    title: "Organised from Delhi",
    text: "Bus, hotel, and itinerary are planned so the group can meet at the pickup point and travel together.",
  },
  {
    title: "Book online, pay securely",
    text: "Choose a trip, add travellers, and complete payment through Razorpay. Confirmation arrives by email.",
  },
  {
    title: "Look up your booking anytime",
    text: "No customer login. Use your booking reference and the email from checkout to view receipt and status.",
  },
];

export function TestimonialsSection() {
  return (
    <section className="bg-surface py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-forest">
            How a JOYWAY trip works
          </p>
          <h2 className="mt-2 font-display text-3xl font-semibold text-charcoal sm:text-4xl">
            Clear steps from booking to departure
          </h2>
          <p className="mt-3 text-sm text-charcoal-muted">
            Product facts — not ratings or attributed reviews.
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {promises.map((item, index) => (
            <InViewReveal
              key={item.title}
              delay={index * 0.1}
              className="relative flex flex-col rounded-2xl border border-border bg-white p-6 shadow-sm"
            >
              <Quote size={28} className="mb-3 text-forest/20" aria-hidden />
              <h3 className="font-semibold text-charcoal">{item.title}</h3>
              <p className="mt-3 flex-1 text-sm leading-relaxed text-charcoal-muted">{item.text}</p>
            </InViewReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
