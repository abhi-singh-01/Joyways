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
import { AdminAuthError } from "@/lib/api/client";
import { toSlug } from "@/lib/slug";
import { PackageImageManager } from "@/components/admin/PackageImageManager";
import { StatusBadge } from "@/components/ui/StatusBadge";

function packageSubmitLabel(loading: boolean, mode: "create" | "edit"): string {
  if (loading) return "Saving...";
  if (mode === "create") return "Save and add photos";
  return "Save changes";
}

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

function FieldHint({ children }: Readonly<{ children: React.ReactNode }>) {
  return <p className="mt-1 text-xs leading-relaxed text-charcoal-muted">{children}</p>;
}

export function PackageForm({ initial, mode }: Readonly<PackageFormProps>) {
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
  const [slugManual, setSlugManual] = useState(mode === "edit");
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

  function handleTitleChange(title: string) {
    setForm((prev) => ({
      ...prev,
      title,
      slug: slugManual ? prev.slug : toSlug(title),
    }));
  }

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      if (mode === "create") {
        const created = await createAdminPackage(form);
        router.push(`/admin/packages/${created.id}/edit`);
      } else if (initial) {
        await updateAdminPackage(initial.id, form);
        router.refresh();
      }
    } catch (err) {
      if (err instanceof AdminAuthError) {
        setError(`${err.message} Sign out and sign in again, then retry.`);
        return;
      }
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

  const slugPreview = form.slug || toSlug(form.title) || "your-package-name";

  return (
    <div className="space-y-8">
      {mode === "edit" && initial && (
        <div className="rounded-2xl border border-border bg-white p-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
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
                  Publish to website
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
          {status === "DRAFT" && (
            <p className="mt-3 text-sm text-charcoal-muted">
              This package is coming soon and hidden on the public website until you
              publish it.
            </p>
          )}
        </div>
      )}

      {mode === "create" && (
        <p className="text-sm text-charcoal-muted">
          New packages start as Coming Soon. Fill the required fields marked with *,
          save the package, add photos, then publish when you are ready for it to
          appear on the website.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Package name
          </h2>
          <p className="mt-1 text-sm text-charcoal-muted">
            This is how the trip appears in the admin list and on the website.
          </p>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Label htmlFor="title">Package title *</Label>
              <Input
                id="title"
                required
                value={form.title}
                onChange={(e) => handleTitleChange(e.target.value)}
                placeholder="e.g. Nainital Weekend Escape"
              />
              <FieldHint>Use a short, clear name travellers will recognise.</FieldHint>
            </div>
            <div>
              <Label htmlFor="destination">Destination *</Label>
              <Input
                id="destination"
                required
                value={form.destination}
                onChange={(e) => updateField("destination", e.target.value)}
                placeholder="e.g. Nainital"
              />
              <FieldHint>City or region the group is travelling to.</FieldHint>
            </div>
            <div>
              <Label htmlFor="slug">Website page address</Label>
              <Input
                id="slug"
                value={form.slug ?? ""}
                onChange={(e) => {
                  setSlugManual(true);
                  updateField("slug", e.target.value);
                }}
                placeholder="auto-generated from the title"
              />
              <FieldHint>
                Optional. Shown in the URL as joyway.in/trips/{slugPreview}. Leave
                blank to generate it from the title.
              </FieldHint>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Descriptions
          </h2>
          <p className="mt-1 text-sm text-charcoal-muted">
            Short text is for cards and lists. Full details appear on the trip page.
          </p>
          <div className="mt-4 space-y-4">
            <div>
              <Label htmlFor="shortDescription">Short summary</Label>
              <Textarea
                id="shortDescription"
                value={form.shortDescription}
                onChange={(e) => updateField("shortDescription", e.target.value)}
                rows={2}
                placeholder="e.g. A 3-day group trip from Delhi to the lakes and hills of Nainital."
              />
              <FieldHint>
                1–2 sentences. Shown on trip cards. Keep it under about 160 characters.
              </FieldHint>
            </div>
            <div>
              <Label htmlFor="fullDescription">Full trip description</Label>
              <Textarea
                id="fullDescription"
                value={form.fullDescription}
                onChange={(e) => updateField("fullDescription", e.target.value)}
                rows={6}
                placeholder="Describe the experience, who it is for, and what makes this trip special."
              />
              <FieldHint>
                Shown on the public trip page. Write a few paragraphs travellers can
                read before booking.
              </FieldHint>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Dates, price and seats
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="departureCity">Departure city *</Label>
              <Input
                id="departureCity"
                required
                value={form.departureCity}
                onChange={(e) => updateField("departureCity", e.target.value)}
              />
              <FieldHint>Where the group starts from, usually Delhi.</FieldHint>
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
              <FieldHint>Write it as travellers should see it, e.g. 3 Days / 2 Nights.</FieldHint>
            </div>
            <div>
              <Label htmlFor="startDate">Start date *</Label>
              <Input
                id="startDate"
                type="date"
                required
                value={form.startDate}
                onChange={(e) => updateField("startDate", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="endDate">End date *</Label>
              <Input
                id="endDate"
                type="date"
                required
                value={form.endDate}
                onChange={(e) => updateField("endDate", e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="pricePerPerson">Price per person (₹) *</Label>
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
              <Label htmlFor="totalSeats">Total seats *</Label>
              <Input
                id="totalSeats"
                type="number"
                min={1}
                required
                value={form.totalSeats || ""}
                onChange={(e) => updateField("totalSeats", Number(e.target.value))}
              />
              <FieldHint>Maximum travellers this package can take.</FieldHint>
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="pickupPoints">Pickup points</Label>
              <Input
                id="pickupPoints"
                placeholder="Connaught Place, Karol Bagh"
                value={form.pickupPoints}
                onChange={(e) => updateField("pickupPoints", e.target.value)}
              />
              <FieldHint>Comma-separated locations where travellers can board.</FieldHint>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            Stay and transport
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="hotelName">Hotel name</Label>
              <Input
                id="hotelName"
                value={form.hotelName}
                onChange={(e) => updateField("hotelName", e.target.value)}
                placeholder="e.g. The Naini Retreat"
              />
            </div>
            <div>
              <Label htmlFor="vehicleType">Vehicle type</Label>
              <Input
                id="vehicleType"
                value={form.vehicleType}
                onChange={(e) => updateField("vehicleType", e.target.value)}
              />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="hotelDetails">Hotel details</Label>
              <Textarea
                id="hotelDetails"
                value={form.hotelDetails}
                onChange={(e) => updateField("hotelDetails", e.target.value)}
                rows={2}
                placeholder="Room type, meals included, location notes"
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-charcoal">
            What is included
          </h2>
          <p className="mt-1 text-sm text-charcoal-muted">
            Put each item on its own line. These lists are shown as bullets on the
            trip page.
          </p>
          <div className="mt-4 space-y-4">
            <div>
              <Label htmlFor="itinerary">Day-by-day itinerary</Label>
              <Textarea
                id="itinerary"
                value={form.itinerary}
                onChange={(e) => updateField("itinerary", e.target.value)}
                rows={5}
                placeholder={"Day 1: Depart Delhi, arrive Nainital\nDay 2: Lake tour and local sightseeing\nDay 3: Return to Delhi"}
              />
              <FieldHint>One day per line, starting with Day 1, Day 2, and so on.</FieldHint>
            </div>
            <div>
              <Label htmlFor="inclusions">What is included</Label>
              <Textarea
                id="inclusions"
                value={form.inclusions}
                onChange={(e) => updateField("inclusions", e.target.value)}
                rows={4}
                placeholder={"AC Tempo Traveller from Delhi\nHotel stay with breakfast\nLocal sightseeing"}
              />
              <FieldHint>
                One item per line. Example: transport, hotel, meals, sightseeing.
              </FieldHint>
            </div>
            <div>
              <Label htmlFor="exclusions">What is not included</Label>
              <Textarea
                id="exclusions"
                value={form.exclusions}
                onChange={(e) => updateField("exclusions", e.target.value)}
                rows={4}
                placeholder={"Personal expenses\nLunch and dinner\nEntry tickets not listed above"}
              />
              <FieldHint>
                One item per line. Things travellers must pay extra for.
              </FieldHint>
            </div>
            <div>
              <Label htmlFor="cancellationPolicy">Cancellation policy</Label>
              <Textarea
                id="cancellationPolicy"
                value={form.cancellationPolicy}
                onChange={(e) => updateField("cancellationPolicy", e.target.value)}
                rows={3}
                placeholder="Full refund if cancelled 7 days before departure. 50% refund within 7 days."
              />
            </div>
          </div>
        </section>

        {error && (
          <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
        )}

        <div className="flex gap-3">
          <Button type="submit" disabled={loading}>
            {packageSubmitLabel(loading, mode)}
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
