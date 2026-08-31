import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";

type JoywayLogoProps = {
  /** Rendered logo height in pixels. Width scales from the asset aspect ratio. */
  height?: number;
  className?: string;
  priority?: boolean;
  /** Wrap in a home link. Pass null to render the image only. */
  href?: string | null;
};

export function JoywayLogo({
  height = 40,
  className,
  priority = false,
  href = "/",
}: JoywayLogoProps) {
  const image = (
    <Image
      src="/logo.png"
      alt="JOYWAY — Explore the world"
      width={Math.round(height * 3.2)}
      height={height}
      priority={priority}
      className={cn("h-auto w-auto max-w-none object-contain", className)}
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
