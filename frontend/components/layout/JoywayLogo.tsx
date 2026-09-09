import Link from "next/link";
import { cn } from "@/lib/utils";

/** Intrinsic size of public/logo.webp */
const LOGO_WIDTH = 721;
const LOGO_HEIGHT = 346;
const LOGO_ASPECT = LOGO_WIDTH / LOGO_HEIGHT;

type JoywayLogoProps = {
  /** Rendered logo height in pixels. */
  height?: number;
  className?: string;
  priority?: boolean;
  /** Wrap in a home link. Pass null to render the image only. */
  href?: string | null;
  /** Use onDark for charcoal backgrounds (footer, admin sidebar). */
  variant?: "default" | "onDark";
  /**
   * compact = compass icon + wordmark (navbar, footer, admin).
   * full = horizontal logo image (large brand sections).
   */
  layout?: "compact" | "full";
  /** Show tagline under wordmark in compact layout */
  showTagline?: boolean;
};

function JoywayWordmark({
  variant,
  iconSize,
  showTagline,
}: {
  variant: "default" | "onDark";
  iconSize: number;
  showTagline: boolean;
}) {
  const onDark = variant === "onDark";

  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src="/web-log.webp"
        alt=""
        aria-hidden
        width={iconSize}
        height={iconSize}
        className={cn(
          "shrink-0 object-contain",
          onDark && "rounded-lg bg-white p-1"
        )}
        style={{ width: iconSize, height: iconSize }}
      />
      <div className="min-w-0 text-left leading-tight">
        <span
          className={cn(
            "block font-semibold tracking-tight",
            iconSize >= 64 ? "text-4xl" : iconSize >= 48 ? "text-2xl" : iconSize >= 40 ? "text-xl" : "text-lg"
          )}
        >
          <span className={onDark ? "text-white" : "text-forest-dark"}>Joy</span>
          <span className={onDark ? "text-accent-light" : "text-forest-light"}>way</span>
        </span>
        {showTagline && (
          <span
            className={cn(
              "mt-0.5 block font-medium uppercase tracking-[0.22em]",
              iconSize >= 64 ? "text-xs" : "text-[10px]",
              onDark ? "text-white/55" : "text-charcoal-muted"
            )}
          >
            Explore the world
          </span>
        )}
      </div>
    </>
  );
}

export function JoywayLogo({
  height = 40,
  className,
  priority = false,
  href = "/",
  variant = "default",
  layout = "full",
  showTagline = false,
}: JoywayLogoProps) {
  const content =
    layout === "compact" ? (
      <span
        className={cn("inline-flex max-w-full items-center gap-2.5", className)}
      >
        <JoywayWordmark
          variant={variant}
          iconSize={height}
          showTagline={showTagline}
        />
      </span>
    ) : (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src="/logo.webp"
        alt="JOYWAY — Explore the world"
        width={Math.round(height * LOGO_ASPECT)}
        height={height}
        decoding="async"
        loading={priority ? "eager" : "lazy"}
        fetchPriority={priority ? "high" : "auto"}
        className={cn(
          "block h-auto max-w-none object-contain",
          variant === "default" && "mix-blend-multiply",
          variant === "onDark" && "rounded-lg bg-white px-3 py-1.5",
          className
        )}
        style={{ height, width: "auto" }}
      />
    );

  if (href === null) {
    return content;
  }

  return (
    <Link href={href} className="inline-flex shrink-0 items-center">
      {content}
    </Link>
  );
}
