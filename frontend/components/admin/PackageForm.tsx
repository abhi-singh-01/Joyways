"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Textarea } from "@/components/ui/Textarea";
import {
  createAdminPackage,
  updateAdminPackage,
  updatePackageStatus,
  type AdminPackageDetail,
  type CreatePackagePayload,
  type PackageStatus,
} from "@/lib/api/adminPackagesApi";
import { PackageImageManager } from "@/components/admin/PackageImageManager";
import { StatusBadge } from "@/components/ui/StatusBadge";

type PackageFormProps = {
  initial?: AdminPackageDetail;
  mode: "create" | "edit";
};

const emptyForm: CreatePackagePayload = {
  title: "",
  destination: "",
  departureCity: "Delhi",
  startDate: "",
  endDate: "",
  duration: "",
  pricePerPerson: 0,
  totalSeats: 20,
  shortDescription: "",
  fullDescription: "",
  pickupPoints: "",
  hotelName: "",
  hotelDetails: "",
  vehicleType: "Tempo Traveller (12+1)",
  itinerary: "",
  inclusions: "",
  exclusions: "",
  cancellationPolicy: "",
};

export function PackageForm({ initial, mode }: PackageFormProps) {
  const router = useRouter();
  const [form, setForm] = useState<CreatePackagePayload>(() =>
    initial
      ? {
          title: initial.title,
          slug: initial.slug,
          destination: initial.destination,
          shortDescription: initial.shortDescription ?? "",
          fullDescription: initial.fullDescription ?? "",
          departureCity: initial.departureCity,
          pickupPoints: initial.pickupPoints ?? "",
          startDate: initial.startDate,
          endDate: initial.endDate,
          duration: initial.duration,
          pricePerPerson: Number(initial.pricePerPerson),
          totalSeats: initial.totalSeats,
          hotelName: initial.hotelName ?? "",
          hotelDetails: initial.hotelDetails ?? "",
          vehicleType: initial.vehicleType ?? "",
          itinerary: initial.itinerary ?? "",
          inclusions: initial.inclusions ?? "",
          exclusions: initial.exclusions ?? "",
          cancellationPolicy: initial.cancellationPolicy ?? "",
        }
      : emptyForm
  );
  const [status, setStatus] = useState<PackageStatus>(initial?.status ?? "DRAFT");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [images, setImages] = useState(initial?.images ?? []);

  function updateField<K extends keyof CreatePackagePayload>(
    key: K,
    value: CreatePackagePayload[K]
  ) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      if (mode === "create") {
        const created = await createAdminPackage(form);
        router.push(`/admin/packages/${created.id}/edit`);
      } else if (initial) {
        await updateAdminPackage(initial.id, {
          ...form,
          availableSeats: initial.availableSeats,
        });
        router.refresh();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(newStatus: PackageStatus) {
    if (!initial) return;
    setLoading(true);
    try {
      await updatePackageStatus(initial.id, newStatus);
      setStatus(newStatus);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Status update failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      {mode === "edit" && initial && (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-white p-4">
          <div className="flex items-center gap-3">
            <span className="text-sm text-charcoal-muted">Status:</span>
            <StatusBadge status={status} />
          </div>
          <div className="flex flex-wrap gap-2">
            {status === "DRAFT" && (
              <Button
                type="button"
                size="sm"
                onClick={() => handleStatusChange("PUBLISHED")}
                disabled={loading}
              >
                Publish
              </Button>
            )}
            {status === "PUBLISHED" && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => handleStatusChange("PAUSED")}
                disabled={loading}
              >
                Pause
              </Button>
            )}
            {status === "PAUSED" && (
              <Button
                type="button"
                size="sm"
                onClick={() => handleStatusChange("PUBLISHED")}
                disabled={loading}
              >
                Re-publish
              </Button>
            )}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Basic Information
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Label htmlFor="title">Package Name *</Label>
              <Input
                id="title"
                required
                value={form.title}
                onChange={(e) => updateField("title", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="destination">Destination *</Label>
              <Input
                id="destination"
                required
                value={form.destination}
                onChange={(e) => updateField("destination", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="slug">Slug (optional)</Label>
              <Input
                id="slug"
                value={form.slug ?? ""}
                onChange={(e) => updateField("slug", e.target.value)}
                placeholder="auto-generated from title"
              />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="shortDescription">Short Description</Label>
              <Textarea
                id="shortDescription"
                value={form.shortDescription}
                onChange={(e) => updateField("shortDescription", e.target.value)}
                rows={2}
              />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="fullDescription">Full Description</Label>
              <Textarea
                id="fullDescription"
                value={form.fullDescription}
                onChange={(e) => updateField("fullDescription", e.target.value)}
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Trip Details
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="departureCity">Departure City *</Label>
              <Input
                id="departureCity"
                required
                value={form.departureCity}
                onChange={(e) => updateField("departureCity", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="duration">Duration *</Label>
              <Input
                id="duration"
                required
                placeholder="3 Days / 2 Nights"
                value={form.duration}
                onChange={(e) => updateField("duration", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="startDate">Start Date *</Label>
              <Input
                id="startDate"
                type="date"
                required
                value={form.startDate}
                onChange={(e) => updateField("startDate", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="endDate">End Date *</Label>
              <Input
                id="endDate"
                type="date"
                required
                value={form.endDate}
                onChange={(e) => updateField("endDate", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="pricePerPerson">Price Per Person (₹) *</Label>
              <Input
                id="pricePerPerson"
                type="number"
                min={1}
                required
                value={form.pricePerPerson || ""}
                onChange={(e) =>
                  updateField("pricePerPerson", Number(e.target.value))
                }
              />
            </div>
            <div>
              <Label htmlFor="totalSeats">Total Seats *</Label>
              <Input
                id="totalSeats"
                type="number"
                min={1}
                required
                value={form.totalSeats || ""}
                onChange={(e) => updateField("totalSeats", Number(e.target.value))}
              />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="pickupPoints">Pickup Points</Label>
              <Input
                id="pickupPoints"
                placeholder="Connaught Place, Karol Bagh"
                value={form.pickupPoints}
                onChange={(e) => updateField("pickupPoints", e.target.value)}
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Stay & Transport
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="hotelName">Hotel Name</Label>
              <Input
                id="hotelName"
                value={form.hotelName}
                onChange={(e) => updateField("hotelName", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="vehicleType">Vehicle Type</Label>
              <Input
                id="vehicleType"
                value={form.vehicleType}
                onChange={(e) => updateField("vehicleType", e.target.value)}
              />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="hotelDetails">Hotel Details</Label>
              <Textarea
                id="hotelDetails"
                value={form.hotelDetails}
                onChange={(e) => updateField("hotelDetails", e.target.value)}
                rows={2}
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Itinerary & Policies
          </h2>
          <div className="mt-4 space-y-4">
            <div>
              <Label htmlFor="itinerary">Itinerary (one day per line)</Label>
              <Textarea
                id="itinerary"
                value={form.itinerary}
                onChange={(e) => updateField("itinerary", e.target.value)}
                rows={5}
              />
            </div>
            <div>
              <Label htmlFor="inclusions">Inclusions (one per line)</Label>
              <Textarea
                id="inclusions"
                value={form.inclusions}
                onChange={(e) => updateField("inclusions", e.target.value)}
                rows={4}
              />
            </div>
            <div>
              <Label htmlFor="exclusions">Exclusions (one per line)</Label>
              <Textarea
                id="exclusions"
                value={form.exclusions}
                onChange={(e) => updateField("exclusions", e.target.value)}
                rows={4}
              />
            </div>
            <div>
              <Label htmlFor="cancellationPolicy">Cancellation Policy</Label>
              <Textarea
                id="cancellationPolicy"
                value={form.cancellationPolicy}
                onChange={(e) => updateField("cancellationPolicy", e.target.value)}
                rows={3}
              />
            </div>
          </div>
        </section>

        {error && (
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
        )}

        <div className="flex gap-3">
          <Button type="submit" disabled={loading}>
            {loading ? "Saving..." : mode === "create" ? "Create Package" : "Save Changes"}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/admin/packages")}
          >
            Cancel
          </Button>
        </div>
      </form>

      {mode === "edit" && initial && (
        <PackageImageManager
          packageId={initial.id}
          images={images}
          onImagesChange={setImages}
        />
      )}
    </div>
  );
}
