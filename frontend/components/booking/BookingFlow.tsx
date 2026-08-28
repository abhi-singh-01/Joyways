"use client";

import { useState } from "react";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import { Minus, Plus, ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { BookingStepper } from "@/components/booking/BookingStepper";
import { TravellerForm } from "@/components/booking/TravellerForm";
import { BookingReviewCard } from "@/components/booking/BookingReviewCard";
import { ErrorState } from "@/components/ui/ErrorState";
import { formatDateRange, formatPrice } from "@/lib/format";
import type { PackageDetail } from "@/lib/api/packagesApi";
import {
  createBooking,
  previewBooking,
  type BookingReview,
  type BookingResult,
  type CustomerInput,
  type TravellerInput,
} from "@/lib/api/bookingApi";
import { PaymentCheckout } from "@/components/booking/PaymentCheckout";

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

type BookingFlowProps = {
  pkg: PackageDetail;
};

export function BookingFlow({ pkg }: BookingFlowProps) {
  const [step, setStep] = useState(1);
  const [count, setCount] = useState(1);
  const [customer, setCustomer] = useState<CustomerInput>({
    fullName: "",
    email: "",
    mobile: "",
  });
  const [travellers, setTravellers] = useState<TravellerInput[]>([emptyTraveller()]);
  const [documents, setDocuments] = useState<(File | null)[]>([null]);
  const [review, setReview] = useState<BookingReview | null>(null);
  const [result, setResult] = useState<BookingResult | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (!pkg.bookable) {
    return (
      <ErrorState
        title="Not available"
        message="This trip is not available for booking."
        fullPage
      />
    );
  }

  function updateCount(next: number) {
    const clamped = Math.max(1, Math.min(next, pkg.availableSeats, 20));
    setCount(clamped);
    setTravellers((prev) => {
      const updated = [...prev];
      while (updated.length < clamped) updated.push(emptyTraveller());
      return updated.slice(0, clamped);
    });
    setDocuments((prev) => {
      const updated = [...prev];
      while (updated.length < clamped) updated.push(null);
      return updated.slice(0, clamped);
    });
  }

  function buildPayload(): Parameters<typeof previewBooking>[0] {
    return {
      packageId: pkg.id,
      numberOfTravellers: count,
      customer,
      travellers: travellers.slice(0, count),
    };
  }

  async function handlePreview() {
    setError("");
    setLoading(true);
    try {
      const data = await previewBooking(buildPayload());
      setReview(data);
      setStep(3);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load review");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit() {
    setError("");
    setLoading(true);
    try {
      const files = documents.slice(0, count).filter((f): f is File => f !== null);
      if (files.length !== count) {
        throw new Error("Please upload ID documents for all travellers.");
      }
      const booking = await createBooking(buildPayload(), files);
      setResult(booking);
      setStep(4);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Booking failed");
    } finally {
      setLoading(false);
    }
  }

  if (result) {
    return (
      <PaymentCheckout
        booking={result}
        customer={customer}
      />
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        href={`/trips/${pkg.slug}`}
        className="mb-6 inline-flex items-center gap-1 text-sm font-medium text-forest hover:text-forest-light"
      >
        <ArrowLeft size={16} />
        Back to trip
      </Link>

      <div className="mb-6 rounded-2xl border border-border bg-white p-5">
        <p className="text-sm font-medium text-forest">{pkg.destination}</p>
        <h1 className="font-display text-2xl font-semibold text-charcoal">{pkg.title}</h1>
        <p className="mt-1 text-sm text-charcoal-muted">
          {formatDateRange(pkg.startDate, pkg.endDate)} · {pkg.duration}
        </p>
        <p className="mt-2 text-lg font-semibold text-forest">
          {formatPrice(pkg.pricePerPerson)}{" "}
          <span className="text-sm font-normal text-charcoal-muted">/ person</span>
        </p>
      </div>

      <BookingStepper currentStep={step} />

      {error && (
        <p className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}

      <AnimatePresence mode="wait">
        {step === 1 && (
          <motion.div
            key="step1"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="rounded-2xl border border-border bg-white p-6"
          >
            <h2 className="font-display text-xl font-semibold text-charcoal">
              How many travellers?
            </h2>
            <p className="mt-1 text-sm text-charcoal-muted">
              {pkg.availableSeats} seats available
            </p>
            <div className="mt-6 flex items-center justify-center gap-6">
              <button
                type="button"
                onClick={() => updateCount(count - 1)}
                disabled={count <= 1}
                className="flex h-12 w-12 items-center justify-center rounded-full border border-border text-charcoal hover:bg-surface disabled:opacity-40"
                aria-label="Decrease travellers"
              >
                <Minus size={20} />
              </button>
              <span className="font-display text-4xl font-semibold text-forest">{count}</span>
              <button
                type="button"
                onClick={() => updateCount(count + 1)}
                disabled={count >= pkg.availableSeats}
                className="flex h-12 w-12 items-center justify-center rounded-full border border-border text-charcoal hover:bg-surface disabled:opacity-40"
                aria-label="Increase travellers"
              >
                <Plus size={20} />
              </button>
            </div>
            <div className="mt-8 flex justify-end">
              <Button onClick={() => setStep(2)}>
                Continue
                <ArrowRight size={16} className="ml-2" />
              </Button>
            </div>
          </motion.div>
        )}

        {step === 2 && (
          <motion.div
            key="step2"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-6"
          >
            <div className="rounded-2xl border border-border bg-white p-5 sm:p-6">
              <h2 className="font-display text-xl font-semibold text-charcoal">
                Primary Contact
              </h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <Label htmlFor="customerName">Full Name *</Label>
                  <Input
                    id="customerName"
                    required
                    value={customer.fullName}
                    onChange={(e) => setCustomer({ ...customer, fullName: e.target.value })}
                  />
                </div>
                <div>
                  <Label htmlFor="customerEmail">Email *</Label>
                  <Input
                    id="customerEmail"
                    type="email"
                    required
                    value={customer.email}
                    onChange={(e) => setCustomer({ ...customer, email: e.target.value })}
                  />
                </div>
                <div>
                  <Label htmlFor="customerMobile">Mobile *</Label>
                  <Input
                    id="customerMobile"
                    type="tel"
                    maxLength={10}
                    required
                    value={customer.mobile}
                    onChange={(e) =>
                      setCustomer({
                        ...customer,
                        mobile: e.target.value.replace(/\D/g, ""),
                      })
                    }
                  />
                </div>
              </div>
            </div>

            {travellers.slice(0, count).map((traveller, index) => (
              <TravellerForm
                key={index}
                index={index}
                data={traveller}
                documentFile={documents[index]}
                onChange={(data) => {
                  const updated = [...travellers];
                  updated[index] = data;
                  setTravellers(updated);
                }}
                onDocumentChange={(file) => {
                  const updated = [...documents];
                  updated[index] = file;
                  setDocuments(updated);
                }}
              />
            ))}

            <div className="flex justify-between gap-3">
              <Button variant="outline" onClick={() => setStep(1)}>
                Back
              </Button>
              <Button onClick={handlePreview} disabled={loading}>
                {loading ? "Loading..." : "Review Booking"}
                <ArrowRight size={16} className="ml-2" />
              </Button>
            </div>
          </motion.div>
        )}

        {step === 3 && review && (
          <motion.div
            key="step3"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-6"
          >
            <BookingReviewCard
              pkg={pkg}
              review={review}
              customerName={customer.fullName}
            />
            <div className="flex justify-between gap-3">
              <Button variant="outline" onClick={() => setStep(2)}>
                Back
              </Button>
              <Button onClick={handleSubmit} disabled={loading} size="lg">
                {loading ? "Creating booking..." : "Confirm Booking"}
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
