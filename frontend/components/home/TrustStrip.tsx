"use client";

import { useEffect, useRef, useState } from "react";
import { Users, MapPin, Star, Calendar } from "lucide-react";

type Stat = {
  icon: React.ElementType;
  value: number;
  suffix: string;
  label: string;
  color: string;
};

const stats: Stat[] = [
  {
    icon: Users,
    value: 500,
    suffix: "+",
    label: "Happy Travelers",
    color: "text-forest",
  },
  {
    icon: MapPin,
    value: 50,
    suffix: "+",
    label: "Trips Completed",
    color: "text-forest",
  },
  {
    icon: Star,
    value: 4.8,
    suffix: "★",
    label: "Average Rating",
    color: "text-accent",
  },
  {
    icon: Calendar,
    value: 2,
    suffix: "+ yrs",
    label: "Creating Memories",
    color: "text-forest",
  },
];

function useCountUp(target: number, duration = 1800, started: boolean) {
  const [count, setCount] = useState(0);
  const isDecimal = target % 1 !== 0;

  useEffect(() => {
    if (!started) return;
    let start: number | null = null;
    const step = (timestamp: number) => {
      if (!start) start = timestamp;
      const progress = Math.min((timestamp - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = eased * target;
      setCount(isDecimal ? Math.round(current * 10) / 10 : Math.floor(current));
      if (progress < 1) requestAnimationFrame(step);
      else setCount(target);
    };
    requestAnimationFrame(step);
  }, [started, target, duration, isDecimal]);

  return count;
}

function StatCard({ stat, started }: { stat: Stat; started: boolean }) {
  const count = useCountUp(stat.value, 1600, started);
  const Icon = stat.icon;

  return (
    <div className="group relative flex flex-col items-center justify-center overflow-hidden rounded-2xl border border-border bg-white px-6 py-8 text-center shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg">
      {/* Soft glow on hover */}
      <div className="pointer-events-none absolute inset-0 rounded-2xl bg-gradient-to-br from-forest/5 via-transparent to-accent/5 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
      <div className="relative z-10">
        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-forest/8 text-forest transition-colors group-hover:bg-forest/15">
          <Icon size={22} />
        </div>
        <p className={`font-display text-3xl font-bold sm:text-4xl ${stat.color}`}>
          {stat.value % 1 !== 0 ? count.toFixed(1) : count}
          <span className="ml-0.5 text-xl">{stat.suffix}</span>
        </p>
        <p className="mt-1 text-sm font-medium text-charcoal-muted">{stat.label}</p>
      </div>
    </div>
  );
}

export function TrustStrip() {
  const sectionRef = useRef<HTMLElement>(null);
  const [started, setStarted] = useState(false);

  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setStarted(true);
          observer.disconnect();
        }
      },
      { threshold: 0.3 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <section ref={sectionRef} className="border-y border-border bg-white py-14 sm:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <p className="mb-8 text-center text-sm font-semibold uppercase tracking-[0.2em] text-charcoal-muted">
          Trusted by travelers across Delhi
        </p>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {stats.map((stat) => (
            <StatCard key={stat.label} stat={stat} started={started} />
          ))}
        </div>
      </div>
    </section>
  );
}
