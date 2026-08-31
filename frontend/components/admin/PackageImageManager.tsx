"use client";

import { useState } from "react";
import { Trash2, Upload } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import {
  deletePackageImage,
  uploadPackageImage,
} from "@/lib/api/adminPackagesApi";
import type { PackageImage } from "@/lib/api/packagesApi";

const MAX_PACKAGE_IMAGE_BYTES = 5 * 1024 * 1024;
const ALLOWED_PACKAGE_IMAGE_TYPES = new Set(["image/jpeg", "image/webp"]);

type PackageImageManagerProps = {
  packageId: number;
  images: PackageImage[];
  onImagesChange: (images: PackageImage[]) => void;
};

function isAllowedPackageImage(file: File): string | null {
  const name = file.name.toLowerCase();
  const typeOk =
    ALLOWED_PACKAGE_IMAGE_TYPES.has(file.type) ||
    name.endsWith(".jpg") ||
    name.endsWith(".jpeg") ||
    name.endsWith(".webp");
  if (!typeOk) {
    return "Only JPEG, JPG, or WebP images are allowed.";
  }
  if (file.size > MAX_PACKAGE_IMAGE_BYTES) {
    return "Each image must be 5MB or smaller.";
  }
  return null;
}

export function PackageImageManager({
  packageId,
  images,
  onImagesChange,
}: PackageImageManagerProps) {
  const [altText, setAltText] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const validationError = isAllowedPackageImage(file);
    if (validationError) {
      setError(validationError);
      e.target.value = "";
      return;
    }
    setLoading(true);
    setError("");
    try {
      const uploaded = await uploadPackageImage(packageId, file, altText || undefined);
      onImagesChange([...images, uploaded]);
      setAltText("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setLoading(false);
      e.target.value = "";
    }
  }

  async function handleDelete(imageId: number) {
    if (!confirm("Delete this image?")) return;
    setLoading(true);
    try {
      await deletePackageImage(packageId, imageId);
      onImagesChange(images.filter((img) => img.id !== imageId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Delete failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-white p-6">
      <h2 className="font-display text-lg font-semibold text-charcoal">
        Package photos
      </h2>
      <p className="mt-1 text-sm text-charcoal-muted">
        Upload JPEG, JPG, or WebP files only (max 5MB each). The first photo is
        shown as the main image on the website. Image URLs are not accepted.
      </p>

      {images.length > 0 && (
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {images.map((img, index) => (
            <div
              key={img.id}
              className="group relative overflow-hidden rounded-xl border border-border"
            >
              <div className="relative aspect-video bg-surface">
                {/* Native img avoids Next.js remote-host restrictions for uploaded files. */}
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={img.imageUrl}
                  alt={img.altText || "Package image"}
                  className="h-full w-full object-cover"
                />
              </div>
              <div className="flex items-center justify-between p-2">
                <span className="truncate text-xs text-charcoal-muted">
                  {index === 0 ? "Main photo" : img.altText || `Photo ${index + 1}`}
                </span>
                <button
                  type="button"
                  onClick={() => handleDelete(img.id)}
                  className="rounded-lg p-1.5 text-red-600 hover:bg-red-50"
                  aria-label="Delete image"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-6 space-y-4 border-t border-border pt-6">
        <div>
          <Label htmlFor="altText">Photo caption (optional)</Label>
          <Input
            id="altText"
            value={altText}
            onChange={(e) => setAltText(e.target.value)}
            placeholder="e.g. Nainital lake at sunrise"
          />
          <p className="mt-1 text-xs text-charcoal-muted">
            Short text describing the photo for accessibility.
          </p>
        </div>
        <div>
          <Label htmlFor="fileUpload">Upload photo</Label>
          <label className="mt-1 flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-border px-4 py-6 text-sm text-charcoal-muted hover:border-forest hover:text-forest">
            <Upload size={18} />
            {loading ? "Uploading..." : "Choose JPEG, JPG or WebP (max 5MB)"}
            <input
              id="fileUpload"
              type="file"
              accept="image/jpeg,image/webp,.jpg,.jpeg,.webp"
              className="hidden"
              onChange={handleUpload}
              disabled={loading}
            />
          </label>
        </div>
      </div>

      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
    </section>
  );
}
