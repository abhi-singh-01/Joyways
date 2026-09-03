import { Bus, Package, Users, IndianRupee } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

const features = [
  {
    icon: Bus,
    title: "Hassle-Free Travel",
    description: "Transport arranged from Delhi. Just show up and go.",
  },
  {
    icon: Package,
    title: "Complete Packages",
    description: "Stay, transport and itinerary in one package.",
  },
  {
    icon: Users,
    title: "Group Experiences",
    description: "Travel, meet people and create memories.",
  },
  {
    icon: IndianRupee,
    title: "Transparent Pricing",
    description: "Clear pricing before booking. No hidden charges.",
  },
];

export function WhyJoyway() {
  return (
    <section className="bg-forest py-20 text-white sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <h2 className="font-display text-3xl font-semibold sm:text-4xl">
            Why JOYWAY
          </h2>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((feature, index) => (
            <InViewReveal
              key={feature.title}
              delay={index * 0.1}
              className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur-sm"
            >
              <feature.icon size={28} className="text-accent" />
              <h3 className="mt-4 font-display text-lg font-semibold">
                {feature.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-white/70">
                {feature.description}
              </p>
            </InViewReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
