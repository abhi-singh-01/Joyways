import Image from "next/image";

const statements = [
  {
    image: "/images/mountains-peaks.webp",
    alt: "High mountain peaks under open sky",
    line: "Every road has a story.",
  },
  {
    image: "/images/mountains-lake.webp",
    alt: "Still alpine lake in a mountain valley",
    line: "Every destination leaves something behind.",
  },
  {
    image: "/images/mountains-road.webp",
    alt: "Winding highland road",
    line: "Every journey changes the way you see home.",
  },
];

export function StoryChapter() {
  return (
    <section className="bg-off-white py-20 sm:py-28">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="max-w-2xl font-display text-3xl font-semibold text-charcoal sm:text-5xl">
          The road is the story.
        </h2>
        <p className="mt-4 max-w-xl text-charcoal-muted">
          Editorial notes from the landscapes we travel — not attributed customer reviews.
        </p>

        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {statements.map((item) => (
            <figure key={item.line} className="relative min-h-[22rem] overflow-hidden">
              <Image
                src={item.image}
                alt={item.alt}
                fill
                loading="lazy"
                sizes="(max-width: 768px) 100vw, 33vw"
                className="object-cover"
              />
              <figcaption className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/75 to-transparent p-6">
                <p className="font-display text-xl font-semibold text-white">{item.line}</p>
              </figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  );
}
