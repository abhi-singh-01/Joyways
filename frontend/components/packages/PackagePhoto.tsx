import Image from "next/image";
import { cn } from "@/lib/utils";

type PackagePhotoProps = {
  src: string;
  alt: string;
  className?: string;
  sizes?: string;
  priority?: boolean;
};

function isApiPackagePhoto(src: string): boolean {
  try {
    const url = new URL(src, "https://joyway.in");
    return url.pathname.startsWith("/api/files/package-images/");
  } catch {
    return false;
  }
}

/**
 * Local assets use the Next.js optimizer. Package photos on api.joyway.in load
 * directly from the API/CDN to avoid an extra frontend image-proxy hop.
 */
export function PackagePhoto({ src, alt, className, sizes, priority }: Readonly<PackagePhotoProps>) {
  const isLocalAsset = src.startsWith("/");
  const directApiPhoto = isApiPackagePhoto(src);

  if (isLocalAsset || src.startsWith("http")) {
    return (
      <Image
        src={src}
        alt={alt}
        fill
        priority={priority}
        unoptimized={directApiPhoto}
        loading={priority ? undefined : "lazy"}
        className={cn("h-full w-full object-cover", className)}
        sizes={sizes ?? "(max-width: 768px) 100vw, 33vw"}
      />
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} className={cn("h-full w-full object-cover", className)} />
  );
}
