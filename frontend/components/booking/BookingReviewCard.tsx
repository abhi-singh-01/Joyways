"use client";

import { formatDateRange, formatPrice } from "@/lib/format";
import type { BookingReview } from "@/lib/api/bookingApi";
import type { PackageDetail } from "@/lib/api/packagesApi";

type BookingReviewCardProps = {
  pkg: PackageDetail;
  review: BookingReview;
  customerName: string;
};

export function BookingReviewCard({ pkg, review, customerName }: BookingReviewCardProps) {
  return (
    <div className="space-y-4 rounded-2xl border border-border bg-white p-6">
      <h3 className="font-display text-xl font-semibold text-charcoal">
        Booking Summary
      </h3>

      <div className="space-y-3 border-b border-border pb-4 text-sm">
        <div className="flex justify-between gap-4">
          <span className="text-charcoal-muted">Trip</span>
          <span className="text-right font-medium text-charcoal">{pkg.title}</span>
        </div>
        <div className="flex justify-between gap-4">
          <span className="text-charcoal-muted">Destination</span>
          <span className="font-medium text-charcoal">{pkg.destination}</span>
        </div>
        <div className="flex justify-between gap-4">
          <span className="text-charcoal-muted">Travel dates</span>
          <span className="font-medium text-charcoal">
            {formatDateRange(pkg.startDate, pkg.endDate)}
          </span>
        </div>
        <div className="flex justify-between gap-4">
          <span className="text-charcoal-muted">Customer</span>
          <span className="font-medium text-charcoal">{customerName}</span>
        </div>
        <div className="flex justify-between gap-4">
          <span className="text-charcoal-muted">Travellers</span>
          <span className="font-medium text-charcoal">{review.numberOfTravellers}</span>
        </div>
      </div>

      <div className="space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-charcoal-muted">
            {formatPrice(review.pricePerPerson)} × {review.numberOfTravellers}
          </span>
          <span className="text-charcoal">{formatPrice(review.subtotal)}</span>
        </div>
        {review.taxesAndFees > 0 && (
          <div className="flex justify-between">
            <span className="text-charcoal-muted">Taxes & fees</span>
            <span className="text-charcoal">{formatPrice(review.taxesAndFees)}</span>
          </div>
        )}
        <div className="flex justify-between border-t border-border pt-3 text-base font-semibold">
          <span className="text-charcoal">Total</span>
          <span className="text-forest">{formatPrice(review.totalAmount)}</span>
        </div>
      </div>

      <p className="text-xs text-charcoal-muted">
        Price calculated on our servers. Payment will be collected in the next step.
      </p>
    </div>
  );
}
