"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Label } from "@/components/ui/Label";
import { Textarea } from "@/components/ui/Textarea";
import { submitBookingReview } from "@/lib/api/bookingApi";

type TripReviewFormProps = {
  bookingReference: string;
  email: string;
  onSubmitted: () => void;
};

export function TripReviewForm({ bookingReference, email, onSubmitted }: Readonly<TripReviewFormProps>) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await submitBookingReview({
        reference: bookingReference,
        email,
        rating,
        comment: comment.trim() || undefined,
      });
      onSubmitted();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form noValidate onSubmit={(e) => void handleSubmit(e)} className="mt-6 border-t border-border pt-6">
      <h2 className="font-semibold text-charcoal">Rate this trip</h2>
      <p className="mt-1 text-sm text-charcoal-muted">
        Your rating is shown on the trip page after you submit it.
      </p>
      <div className="mt-4">
        <Label htmlFor="trip-rating">Rating</Label>
        <div id="trip-rating" className="mt-2 flex gap-2">
          {[1, 2, 3, 4, 5].map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setRating(value)}
              className={`h-10 w-10 rounded-full border text-sm font-semibold ${
                value <= rating
                  ? "border-forest bg-forest text-white"
                  : "border-border bg-white text-charcoal"
              }`}
              aria-pressed={value === rating}
              aria-label={`${value} star${value === 1 ? "" : "s"}`}
            >
              {value}
            </button>
          ))}
        </div>
      </div>
      <div className="mt-4">
        <Label htmlFor="trip-comment">Comment (optional)</Label>
        <Textarea
          id="trip-comment"
          maxLength={1000}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="What did you enjoy?"
        />
      </div>
      {error && (
        <p className="mt-2 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
      <Button type="submit" className="mt-4" disabled={loading}>
        {loading ? "Submitting..." : "Submit review"}
      </Button>
    </form>
  );
}
