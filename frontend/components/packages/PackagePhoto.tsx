import Image from "next/image";
import { cn } from "@/lib/utils";

type PackagePhotoProps = {
  src: string;
  alt: string;
  className?: string;
  sizes?: string;
  priority?: boolean;
};

/**
 * Unsplash URLs go through the Next.js optimizer. Uploaded files are served from
 * api.joyway.in and must use a plain img so a missing remotePattern cannot hide them.
 */
export function PackagePhoto({ src, alt, className, sizes, priority }: PackagePhotoProps) {
  const optimized = src.includes("images.unsplash.com");

  if (optimized) {
    return (
      <Image
        src={src}
        alt={alt}
        fill
        priority={priority}
        className={cn("object-cover", className)}
        sizes={sizes}
      />
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} className={cn("h-full w-full object-cover", className)} />
  );
}
