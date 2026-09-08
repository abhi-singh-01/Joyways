type PageHeroProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  children?: React.ReactNode;
};

export function PageHero({ eyebrow, title, description, children }: PageHeroProps) {
  return (
    <section className="relative overflow-hidden border-b border-border bg-gradient-to-br from-forest/8 via-off-white to-accent/10">
      <div
        className="pointer-events-none absolute inset-0 opacity-40"
        aria-hidden
        style={{
          backgroundImage:
            "radial-gradient(circle at 20% 20%, var(--color-forest-light) 0%, transparent 45%), radial-gradient(circle at 80% 0%, var(--color-accent) 0%, transparent 35%)",
        }}
      />
      <div className="relative mx-auto max-w-7xl px-4 py-14 sm:px-6 sm:py-20 lg:px-8">
        {eyebrow && (
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-forest">
            {eyebrow}
          </p>
        )}
        <h1 className="mt-2 max-w-3xl font-display text-4xl font-semibold leading-tight text-charcoal sm:text-5xl">
          {title}
        </h1>
        {description && (
          <p className="mt-4 max-w-2xl text-lg leading-relaxed text-charcoal-muted">
            {description}
          </p>
        )}
        {children}
      </div>
    </section>
  );
}
