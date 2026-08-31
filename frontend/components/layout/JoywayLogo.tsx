import Link from "next/link";
import { cn } from "@/lib/utils";

/** Intrinsic size of public/logo.png */
const LOGO_WIDTH = 721;
const LOGO_HEIGHT = 346;
const LOGO_ASPECT = LOGO_WIDTH / LOGO_HEIGHT;

type JoywayLogoProps = {
  /** Rendered logo height in pixels. Width scales from the asset aspect ratio. */
  height?: number;
  className?: string;
  priority?: boolean;
  /** Wrap in a home link. Pass null to render the image only. */
  href?: string | null;
  /** Use onDark for charcoal backgrounds (footer, admin sidebar). */
  variant?: "default" | "onDark";
};

export function JoywayLogo({
  height = 40,
  className,
  priority = false,
  href = "/",
  variant = "default",
}: JoywayLogoProps) {
  const width = Math.round(height * LOGO_ASPECT);

  const image = (
    // Native img avoids next/image layout quirks and keeps local static assets reliable.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src="/logo.png"
      alt="JOYWAY — Explore the world"
      width={width}
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
    return image;
  }

  return (
    <Link href={href} className="inline-flex shrink-0 items-center">
      {image}
    </Link>
  );
}
