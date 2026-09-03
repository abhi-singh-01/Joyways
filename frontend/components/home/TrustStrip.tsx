import { Bus, MapPin, Shield, Sparkles, Users } from "lucide-react";
import { InViewReveal } from "@/components/motion/InViewReveal";

const stats = [
  { icon: MapPin, label: "Delhi departures", value: "Every weekend" },
  { icon: Sparkles, label: "Curated routes", value: "Hills & spiritual" },
  { icon: Bus, label: "All-in packages", value: "Transport + stay" },
  { icon: Users, label: "Group experiences", value: "Meet & travel" },
  { icon: Shield, label: "Secure booking", value: "Online payments" },
];

export function TrustStrip() {
  return (
    <section className="border-y border-border bg-white py-10 sm:py-12">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {stats.map((item, index) => (
            <InViewReveal
              key={item.label}
              delay={index * 0.06}
              duration={0.45}
              y={12}
              className="flex items-center gap-3 rounded-2xl border border-border/80 bg-surface/50 px-4 py-4"
            >
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-forest/10 text-forest">
                <item.icon size={20} />
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-charcoal-muted">
                  {item.label}
                </p>
                <p className="text-sm font-semibold text-charcoal">{item.value}</p>
              </div>
            </InViewReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
