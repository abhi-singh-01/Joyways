"use client";

import { useState } from "react";
import Image from "next/image";
import { Trash2, Upload, Link as LinkIcon } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import {
  addPackageImageUrl,
  deletePackageImage,
  uploadPackageImage,
} from "@/lib/api/adminPackagesApi";
import type { PackageImage } from "@/lib/api/packagesApi";

type PackageImageManagerProps = {
  packageId: number;
  images: PackageImage[];
  onImagesChange: (images: PackageImage[]) => void;
};

export function PackageImageManager({
  packageId,
  images,
  onImagesChange,
}: PackageImageManagerProps) {
  const [imageUrl, setImageUrl] = useState("");
  const [altText, setAltText] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleAddUrl(e: React.FormEvent) {
    e.preventDefault();
    if (!imageUrl.trim()) return;
    setLoading(true);
    setError("");
    try {
      const added = await addPackageImageUrl(packageId, imageUrl.trim(), altText || undefined);
      onImagesChange([...images, added]);
      setImageUrl("");
      setAltText("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add image");
    } finally {
      setLoading(false);
    }
  }

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
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
        Package Images
      </h2>
      <p className="mt-1 text-sm text-charcoal-muted">
        Add images by URL or upload from your device
      </p>

      {images.length > 0 && (
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {images.map((img) => (
            <div
              key={img.id}
              className="group relative overflow-hidden rounded-xl border border-border"
            >
              <div className="relative aspect-video">
                <Image
                  src={img.imageUrl}
                  alt={img.altText || "Package image"}
                  fill
                  className="object-cover"
                  sizes="200px"
                />
              </div>
              <div className="flex items-center justify-between p-2">
                <span className="truncate text-xs text-charcoal-muted">
                  {img.altText || "No alt text"}
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
        <form onSubmit={handleAddUrl} className="space-y-3">
          <div>
            <Label htmlFor="imageUrl">Add by URL</Label>
            <div className="flex gap-2">
              <Input
                id="imageUrl"
                type="url"
                placeholder="https://images.unsplash.com/..."
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
              />
              <Button type="submit" variant="outline" disabled={loading}>
                <LinkIcon size={16} />
              </Button>
            </div>
          </div>
          <div>
            <Label htmlFor="altText">Alt Text (optional)</Label>
            <Input
              id="altText"
              value={altText}
              onChange={(e) => setAltText(e.target.value)}
              placeholder="Describe the image"
            />
          </div>
        </form>

        <div>
          <Label htmlFor="fileUpload">Upload Image</Label>
          <label className="mt-1 flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-border px-4 py-6 text-sm text-charcoal-muted hover:border-forest hover:text-forest">
            <Upload size={18} />
            {loading ? "Processing..." : "Choose JPEG, PNG or WebP (max 5MB)"}
            <input
              id="fileUpload"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              onChange={handleUpload}
              disabled={loading}
            />
          </label>
        </div>
      </div>

      {error && (
        <p className="mt-3 text-sm text-red-600">{error}</p>
      )}
    </section>
  );
}
