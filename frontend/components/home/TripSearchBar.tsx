"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Search } from "lucide-react";

type TripSearchBarProps = {
  className?: string;
};

export function TripSearchBar({ className }: Readonly<TripSearchBarProps>) {
  const router = useRouter();
  const [query, setQuery] = useState("");

  function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = query.trim();
    router.push(trimmed ? `/trips?q=${encodeURIComponent(trimmed)}` : "/trips");
  }

  return (
    <form
      onSubmit={handleSubmit}
      className={
        className ??
        "mx-auto mt-8 flex max-w-xl flex-col gap-3 rounded-2xl border border-border bg-white p-2 shadow-sm sm:flex-row sm:items-center"
      }
    >
      <div className="flex flex-1 items-center gap-2 px-3">
        <Search size={18} className="shrink-0 text-forest" aria-hidden />
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search destinations…"
          className="w-full bg-transparent py-2.5 text-sm text-charcoal outline-none placeholder:text-charcoal-muted"
          aria-label="Search trips by destination"
        />
      </div>
      <button
        type="submit"
        className="rounded-xl bg-forest px-6 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-forest-light"
      >
        Find trips
      </button>
    </form>
  );
}
