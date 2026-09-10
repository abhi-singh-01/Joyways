import { Star } from "lucide-react";
import { formatDate } from "@/lib/format";
import type { PackageReviews } from "@/lib/api/packagesApi";

type PackageReviewsSectionProps = {
  reviews: PackageReviews;
};

function Stars({ rating }: Readonly<{ rating: number }>) {
  return (
    <span className="inline-flex items-center gap-0.5" aria-label={`${rating} out of 5`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          size={14}
          className={star <= rating ? "fill-accent text-accent" : "text-border"}
        />
      ))}
    </span>
  );
}

export function PackageReviewsSection({ reviews }: Readonly<PackageReviewsSectionProps>) {
  return (
    <section>
      <h2 className="font-display text-2xl font-semibold text-charcoal">Traveller reviews</h2>
      {reviews.reviewCount === 0 ? (
        <p className="mt-3 text-sm text-charcoal-muted">
          Reviews appear here after travellers complete this trip.
        </p>
      ) : (
        <>
          <p className="mt-2 text-sm text-charcoal-muted">
            <span className="font-semibold text-charcoal">
              {reviews.averageRating?.toFixed(1)}
            </span>{" "}
            average from {reviews.reviewCount}{" "}
            {reviews.reviewCount === 1 ? "review" : "reviews"}
          </p>
          <ul className="mt-6 space-y-4">
            {reviews.reviews.map((review, index) => (
              <li
                key={`${review.customerName}-${review.createdAt}-${index}`}
                className="rounded-2xl border border-border bg-white p-5"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="font-medium text-charcoal">{review.customerName}</p>
                  <Stars rating={review.rating} />
                </div>
                {review.comment && (
                  <p className="mt-2 text-sm leading-relaxed text-charcoal-muted">
                    {review.comment}
                  </p>
                )}
                <p className="mt-2 text-xs text-charcoal-muted">{formatDate(review.createdAt)}</p>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
