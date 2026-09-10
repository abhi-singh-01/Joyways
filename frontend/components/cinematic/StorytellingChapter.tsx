import Image from "next/image";

const lines = [
  "Wake before sunrise.",
  "Take the road that wasn't on the map.",
  "Stay somewhere you'll remember.",
  "Come back with more than photographs.",
];

export function StorytellingChapter() {
  return (
    <section className="relative min-h-[100svh] overflow-hidden bg-forest-dark text-white">
      <Image
        src="/images/mountains-road.webp"
        alt="A highland road through the mountains at dusk"
        fill
        loading="lazy"
        sizes="100vw"
        className="object-cover"
      />
      <div className="absolute inset-0 bg-black/50" />
      <div className="relative z-10 mx-auto flex min-h-[100svh] max-w-7xl flex-col justify-end px-4 py-20 sm:px-6 lg:px-8">
        <ul className="max-w-xl space-y-6">
          {lines.map((line) => (
            <li key={line} className="font-display text-2xl font-semibold leading-snug sm:text-4xl">
              {line}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
