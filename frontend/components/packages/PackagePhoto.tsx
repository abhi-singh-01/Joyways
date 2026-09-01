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
 * Local assets under /public use the Next.js optimizer. Package photos from Azure
 * (via api.joyway.in/api/files/...) are also optimized when remotePatterns allow.
 */
export function PackagePhoto({ src, alt, className, sizes, priority }: PackagePhotoProps) {
  const isLocalAsset = src.startsWith("/");

  if (isLocalAsset || src.startsWith("http")) {
    return (
      <Image
        src={src}
        alt={alt}
        fill
        priority={priority}
        loading={priority ? undefined : "lazy"}
        className={cn("object-cover", className)}
        sizes={sizes ?? "(max-width: 768px) 100vw, 33vw"}
      />
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} className={cn("h-full w-full object-cover", className)} />
  );
}
