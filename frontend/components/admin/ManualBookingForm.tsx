"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createAdminBooking,
  type AdminBookingPaymentMode,
} from "@/lib/api/adminBookingsApi";
import {
  getAdminPackage,
  listAdminPackages,
  type AdminPackageDetail,
  type AdminPackageSummary,
} from "@/lib/api/adminPackagesApi";
import type { CustomerInput, TravellerInput } from "@/lib/api/bookingApi";
import { TravellerForm } from "@/components/booking/TravellerForm";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import {
  type FieldErrors,
  fieldErrorsFromZod,
  validateCustomer,
  validateIdDocumentReference,
  validateTraveller,
} from "@/lib/formValidation";
import { formatDate, formatPrice, parsePickupPoints } from "@/lib/format";

function emptyTraveller(): TravellerInput {
  return {
    fullName: "",
    dateOfBirth: "",
    gender: "",
    mobile: "",
    email: "",
    emergencyContact: "",
    idDocumentType: "",
    idDocumentReference: "",
  };
}

type TravellerDraft = TravellerInput & { clientKey: string };

function newTravellerDraft(): TravellerDraft {
  return { ...emptyTraveller(), clientKey: crypto.randomUUID() };
}

function toTravellerInput({ clientKey: _clientKey, ...traveller }: TravellerDraft): TravellerInput {
  return traveller;
}

function emptyCustomer(): CustomerInput {
  return { fullName: "", email: "", mobile: "" };
}

function kolkataToday(): string {
  return new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
}

export function ManualBookingForm() {
  const router = useRouter();
  const [packages, setPackages] = useState<AdminPackageSummary[]>([]);
  const [packagesLoading, setPackagesLoading] = useState(true);
  const [packagesError, setPackagesError] = useState("");
  const [selectedPackage, setSelectedPackage] = useState<AdminPackageDetail | null>(null);
  const [packageId, setPackageId] = useState("");
  const [count, setCount] = useState(1);
  const [customer, setCustomer] = useState<CustomerInput>(emptyCustomer);
  const [travellers, setTravellers] = useState<TravellerDraft[]>([newTravellerDraft()]);
  const [customerErrors, setCustomerErrors] = useState<FieldErrors<CustomerInput>>({});
  const [travellerErrors, setTravellerErrors] = useState<FieldErrors<TravellerInput>[]>([{}]);
  const [pickupPoint, setPickupPoint] = useState("");
  const [pickupError, setPickupError] = useState("");
  const [paymentMode, setPaymentMode] = useState<AdminBookingPaymentMode>("PENDING_ONLINE");
  const [offlinePaymentReference, setOfflinePaymentReference] = useState("");
  const [offlineRefError, setOfflineRefError] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submittingRef = useRef(false);
  const countRef = useRef(count);
  countRef.current = count;

  const pickupOptions = useMemo(
    () => parsePickupPoints(selectedPackage?.pickupPoints),
    [selectedPackage?.pickupPoints]
  );

  const bookablePackages = useMemo(() => {
    const today = kolkataToday();
    return packages.filter(
      (pkg) => pkg.status === "PUBLISHED" && pkg.startDate >= today && pkg.availableSeats > 0
    );
  }, [packages]);

  const estimatedTotal =
    selectedPackage != null ? selectedPackage.pricePerPerson * count : null;
  const packageDetailsLoading = Boolean(packageId) && selectedPackage == null && !error;

  useEffect(() => {
    listAdminPackages()
      .then(setPackages)
      .catch((err) => {
        setPackagesError(err instanceof Error ? err.message : "Failed to load trips");
      })
      .finally(() => setPackagesLoading(false));
  }, []);

  useEffect(() => {
    if (!packageId) return;
    let cancelled = false;
    getAdminPackage(Number(packageId))
      .then((detail) => {
        if (cancelled) return;
        setSelectedPackage(detail);
        const points = parsePickupPoints(detail.pickupPoints);
        setPickupPoint(points.length === 1 ? points[0] : "");
        applyTravellerCount(countRef.current, detail.availableSeats);
      })
      .catch((err) => {
        if (cancelled) return;
        setSelectedPackage(null);
        setError(err instanceof Error ? err.message : "Failed to load trip details");
      });
    return () => {
      cancelled = true;
    };
  }, [packageId]);

  function applyTravellerCount(next: number, maxSeats: number) {
    const clamped = Math.min(20, Math.max(1, Math.min(next, Math.max(1, maxSeats))));
    setCount(clamped);
    setTravellers((prev) => {
      const copy = [...prev];
      while (copy.length < clamped) copy.push(newTravellerDraft());
      return copy.slice(0, clamped);
    });
    setTravellerErrors((prev) => {
      const copy = [...prev];
      while (copy.length < clamped) copy.push({});
      return copy.slice(0, clamped);
    });
  }

  function updateCount(next: number) {
    applyTravellerCount(next, selectedPackage?.availableSeats ?? 20);
  }

  function copyCustomerFromFirstTraveller() {
    const first = travellers[0];
    if (!first) return;
    setCustomer({
      fullName: first.fullName,
      email: first.email,
      mobile: first.mobile,
    });
    setCustomerErrors({});
  }

  function validate(): boolean {
    const customerResult = validateCustomer(customer);
    const nextCustomerErrors = customerResult.success
      ? {}
      : fieldErrorsFromZod(customerResult);
    const nextTravellerErrors: FieldErrors<TravellerInput>[] = [];
    let valid = customerResult.success;

    for (let i = 0; i < count; i++) {
      const traveller = travellers[i] ? toTravellerInput(travellers[i]) : emptyTraveller();
      const travellerResult = validateTraveller(traveller);
      const fieldErrors = travellerResult.success
        ? {}
        : fieldErrorsFromZod(travellerResult);
      const refError = validateIdDocumentReference(
        traveller.idDocumentType,
        traveller.idDocumentReference ?? ""
      );
      if (refError) {
        fieldErrors.idDocumentReference = refError;
      }
      nextTravellerErrors.push(fieldErrors);
      if (!travellerResult.success || refError) {
        valid = false;
      }
    }

    let nextPickupError = "";
    if (pickupOptions.length > 0 && !pickupPoint.trim()) {
      nextPickupError = "Choose a pickup point";
      valid = false;
    }

    let nextOfflineError = "";
    if (paymentMode === "PAID_OFFLINE" && !offlinePaymentReference.trim()) {
      nextOfflineError = "Enter the UPI, cash receipt, or bank transfer reference";
      valid = false;
    }

    setCustomerErrors(nextCustomerErrors);
    setTravellerErrors(nextTravellerErrors);
    setPickupError(nextPickupError);
    setOfflineRefError(nextOfflineError);
    return valid;
  }

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    if (saving || submittingRef.current) return;
    setError("");
    if (!packageId) {
      setError("Select a published trip.");
      return;
    }
    if (!selectedPackage) {
      setError("Wait for the trip details to load, then try again.");
      return;
    }
    if (!validate()) {
      setError("Please fix the highlighted fields.");
      return;
    }

    submittingRef.current = true;
    setSaving(true);
    try {
      const booking = await createAdminBooking({
        packageId: Number(packageId),
        numberOfTravellers: count,
        customer,
        travellers: travellers.slice(0, count).map(toTravellerInput),
        pickupPoint: pickupPoint.trim() || undefined,
        paymentMode,
        offlinePaymentReference:
          paymentMode === "PAID_OFFLINE" ? offlinePaymentReference.trim() : undefined,
        notes: notes.trim() || undefined,
      });
      router.push(`/admin/bookings/${booking.bookingId}`);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create booking");
    } finally {
      submittingRef.current = false;
      setSaving(false);
    }
  }

  if (packagesError) {
    return (
      <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{packagesError}</p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-3xl space-y-8">
      {error && (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}

      <section className="space-y-4">
        <h2 className="font-display text-xl font-semibold text-charcoal">Trip</h2>
        <label className="block">
          <span className="text-sm font-medium text-charcoal">Package</span>
          <select
            required
            value={packageId}
            onChange={(e) => {
              setPackageId(e.target.value);
              setSelectedPackage(null);
              setPickupPoint("");
              setError("");
            }}
            className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          >
            <option value="">Select a published trip</option>
            {bookablePackages.map((pkg) => (
              <option key={pkg.id} value={pkg.id}>
                {pkg.title} · {pkg.destination} · {formatDate(pkg.startDate)} ·{" "}
                {pkg.availableSeats} seats · {formatPrice(pkg.pricePerPerson)}
              </option>
            ))}
          </select>
        </label>
        {packagesLoading && (
          <p className="text-sm text-charcoal-muted">Loading published trips...</p>
        )}
        {packageDetailsLoading && (
          <p className="text-sm text-charcoal-muted">Loading trip details...</p>
        )}
        {!packagesLoading && bookablePackages.length === 0 && (
          <p className="text-sm text-charcoal-muted">
            No published upcoming trips with open seats. Publish a package first.
          </p>
        )}
        {selectedPackage && (
          <p className="text-sm text-charcoal-muted">
            {selectedPackage.destination} · {formatDate(selectedPackage.startDate)} –{" "}
            {formatDate(selectedPackage.endDate)} · {selectedPackage.availableSeats} seats left
            {estimatedTotal != null ? ` · Total ${formatPrice(estimatedTotal)}` : ""}
          </p>
        )}
        <label className="block max-w-xs">
          <span className="text-sm font-medium text-charcoal">Travellers</span>
          <input
            type="number"
            min={1}
            max={Math.min(20, selectedPackage?.availableSeats ?? 20)}
            value={count}
            onChange={(e) => updateCount(Number(e.target.value) || 1)}
            className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          />
        </label>
        {pickupOptions.length > 0 && (
          <div>
            <Label htmlFor="admin-pickup">Pickup point *</Label>
            <Select
              id="admin-pickup"
              required
              value={pickupPoint}
              error={pickupError}
              onChange={(e) => {
                setPickupPoint(e.target.value);
                setPickupError("");
              }}
            >
              <option value="">Select a pickup point</option>
              {pickupOptions.map((point) => (
                <option key={point} value={point}>
                  {point}
                </option>
              ))}
            </Select>
          </div>
        )}
      </section>

      <section className="space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 className="font-display text-xl font-semibold text-charcoal">Customer</h2>
          <button
            type="button"
            onClick={copyCustomerFromFirstTraveller}
            className="text-sm font-medium text-forest hover:underline"
          >
            Copy from traveller 1
          </button>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Label htmlFor="customer-name">Full name *</Label>
            <Input
              id="customer-name"
              required
              value={customer.fullName}
              error={customerErrors.fullName}
              onChange={(e) => setCustomer({ ...customer, fullName: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="customer-email">Email *</Label>
            <Input
              id="customer-email"
              type="email"
              required
              value={customer.email}
              error={customerErrors.email}
              onChange={(e) => setCustomer({ ...customer, email: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="customer-mobile">Mobile *</Label>
            <Input
              id="customer-mobile"
              type="tel"
              required
              inputMode="numeric"
              maxLength={10}
              value={customer.mobile}
              error={customerErrors.mobile}
              onChange={(e) =>
                setCustomer({ ...customer, mobile: e.target.value.replace(/\D/g, "") })
              }
            />
          </div>
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="font-display text-xl font-semibold text-charcoal">Travellers</h2>
        <p className="text-sm text-charcoal-muted">
          ID scans are optional for admin bookings. Enter a masked ID reference (last 4 digits for Aadhaar).
        </p>
        {travellers.slice(0, count).map((traveller, index) => (
          <TravellerForm
            key={traveller.clientKey}
            index={index}
            data={toTravellerInput(traveller)}
            documentFile={null}
            errors={travellerErrors[index]}
            requireDocument={false}
            onChange={(data) => {
              setTravellers((prev) =>
                prev.map((item) =>
                  item.clientKey === traveller.clientKey
                    ? { ...data, clientKey: item.clientKey }
                    : item
                )
              );
            }}
            onDocumentChange={() => undefined}
          />
        ))}
      </section>

      <section className="space-y-4">
        <h2 className="font-display text-xl font-semibold text-charcoal">Payment</h2>
        <fieldset className="space-y-3">
          <legend className="sr-only">Payment mode</legend>
          <div className="flex cursor-pointer items-start gap-3 rounded-2xl border border-border bg-white p-4">
            <input
              id="payment-mode-online"
              type="radio"
              name="paymentMode"
              className="mt-1"
              checked={paymentMode === "PENDING_ONLINE"}
              onChange={() => setPaymentMode("PENDING_ONLINE")}
            />
            <label htmlFor="payment-mode-online" className="cursor-pointer">
              <span className="block font-medium text-charcoal">Customer will pay online</span>
              <span className="mt-1 block text-sm text-charcoal-muted">
                Seats are held. Share the booking reference and email so they can pay via booking lookup.
              </span>
            </label>
          </div>
          <div className="flex cursor-pointer items-start gap-3 rounded-2xl border border-border bg-white p-4">
            <input
              id="payment-mode-offline"
              type="radio"
              name="paymentMode"
              className="mt-1"
              checked={paymentMode === "PAID_OFFLINE"}
              onChange={() => setPaymentMode("PAID_OFFLINE")}
            />
            <label htmlFor="payment-mode-offline" className="cursor-pointer">
              <span className="block font-medium text-charcoal">Already paid offline</span>
              <span className="mt-1 block text-sm text-charcoal-muted">
                Cash, UPI, or bank transfer collected by staff. Confirms the booking immediately.
              </span>
            </label>
          </div>
        </fieldset>
        {paymentMode === "PAID_OFFLINE" && (
          <div>
            <Label htmlFor="offline-ref">Payment reference *</Label>
            <Input
              id="offline-ref"
              required
              maxLength={100}
              placeholder="UPI ID, cash receipt no., or bank UTR"
              value={offlinePaymentReference}
              error={offlineRefError}
              onChange={(e) => {
                setOfflinePaymentReference(e.target.value);
                setOfflineRefError("");
              }}
            />
          </div>
        )}
        <div>
          <Label htmlFor="admin-notes">Internal notes</Label>
          <textarea
            id="admin-notes"
            rows={3}
            maxLength={500}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Optional — stored on the audit log"
            className="mt-1 w-full rounded-xl border border-border px-4 py-2.5 text-sm"
          />
        </div>
      </section>

      <button
        type="submit"
        disabled={
          saving ||
          packagesLoading ||
          packageDetailsLoading ||
          bookablePackages.length === 0
        }
        className="rounded-xl bg-forest px-5 py-2.5 text-sm font-medium text-white disabled:opacity-50"
      >
        {saving ? "Creating booking..." : "Create booking"}
      </button>
    </form>
  );
}
