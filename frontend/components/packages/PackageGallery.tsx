"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import type { PackageImage } from "@/lib/api/packagesApi";
import { cn } from "@/lib/utils";
import { PackagePhoto } from "@/components/packages/PackagePhoto";

type PackageGalleryProps = {
  images: PackageImage[];
  destination: string;
  className?: string;
};

export function PackageGallery({ images, destination, className }: Readonly<PackageGalleryProps>) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  if (images.length === 0) {
    return (
      <div
        className={cn(
          "flex aspect-[16/9] items-center justify-center rounded-2xl bg-surface",
          className
        )}
      >
        <p className="text-charcoal-muted">No images available</p>
      </div>
    );
  }

  const active = images[activeIndex];

  const goPrev = () =>
    setActiveIndex((i) => (i === 0 ? images.length - 1 : i - 1));
  const goNext = () =>
    setActiveIndex((i) => (i === images.length - 1 ? 0 : i + 1));

  return (
    <>
      <div className={cn("space-y-3", className)}>
        <div className="group relative aspect-[16/9] overflow-hidden rounded-2xl bg-surface">
          <button
            type="button"
            className="relative h-full w-full cursor-zoom-in"
            onClick={() => setLightboxOpen(true)}
          >
            <PackagePhoto
              src={active.imageUrl}
              alt={active.altText || destination}
              priority
              sizes="(max-width: 1024px) 100vw, 70vw"
            />
          </button>

          {images.length > 1 && (
            <>
              <button
                type="button"
                onClick={goPrev}
                className="absolute left-3 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-charcoal shadow-md opacity-100 transition-opacity md:opacity-0 md:group-hover:opacity-100 md:focus-visible:opacity-100"
                aria-label="Previous image"
              >
                <ChevronLeft size={20} />
              </button>
              <button
                type="button"
                onClick={goNext}
                className="absolute right-3 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-charcoal shadow-md opacity-100 transition-opacity md:opacity-0 md:group-hover:opacity-100 md:focus-visible:opacity-100"
                aria-label="Next image"
              >
                <ChevronRight size={20} />
              </button>
            </>
          )}
        </div>

        {images.length > 1 && (
          <div className="flex gap-2 overflow-x-auto pb-1">
            {images.map((img, idx) => (
              <button
                key={img.id}
                type="button"
                onClick={() => setActiveIndex(idx)}
                className={cn(
                  "relative h-16 w-24 shrink-0 overflow-hidden rounded-lg border-2 transition-colors",
                  idx === activeIndex
                    ? "border-forest"
                    : "border-transparent opacity-70 hover:opacity-100"
                )}
                aria-label={`View image ${idx + 1}`}
                aria-current={idx === activeIndex}
              >
                <PackagePhoto
                  src={img.imageUrl}
                  alt={img.altText || ""}
                  sizes="96px"
                />
              </button>
            ))}
          </div>
        )}
      </div>

      <AnimatePresence>
        {lightboxOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center bg-charcoal/90 p-4"
            onClick={() => setLightboxOpen(false)}
          >
            <button
              type="button"
              className="absolute right-4 top-4 z-10 rounded-full bg-white/10 p-2 text-white hover:bg-white/20"
              onClick={() => setLightboxOpen(false)}
              aria-label="Close gallery"
            >
              <X size={24} />
            </button>
            <div className="pointer-events-none relative h-[70vh] w-full max-w-5xl">
              <PackagePhoto
                src={active.imageUrl}
                alt={active.altText || destination}
                className="object-contain"
                sizes="100vw"
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
