"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import { joinWaitlist } from "@/lib/api/packagesApi";
import {
  customerSchema,
  INDIAN_MOBILE_REGEX,
  type FieldErrors,
} from "@/lib/formValidation";

type WaitlistFormProps = {
  slug: string;
  destination: string;
};

export function WaitlistForm({ slug, destination }: Readonly<WaitlistFormProps>) {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [mobile, setMobile] = useState("");
  const [seatsRequested, setSeatsRequested] = useState(1);
  const [errors, setErrors] = useState<FieldErrors<{ fullName: string; email: string; mobile: string }>>(
    {}
  );
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [joined, setJoined] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    const parsed = customerSchema.pick({ fullName: true, email: true }).safeParse({
      fullName,
      email,
    });
    const nextErrors: FieldErrors<{ fullName: string; email: string; mobile: string }> = {};
    if (!parsed.success) {
      for (const issue of parsed.error.issues) {
        const key = issue.path[0];
        if (key === "fullName" || key === "email") {
          nextErrors[key] = issue.message;
        }
      }
    }
    if (mobile && !INDIAN_MOBILE_REGEX.test(mobile)) {
      nextErrors.mobile = "Enter a valid 10-digit Indian mobile number";
    }
    if (nextErrors.fullName || nextErrors.email || nextErrors.mobile) {
      setErrors(nextErrors);
      setError("Please fix the highlighted fields.");
      return;
    }

    setErrors({});
    setError("");
    setLoading(true);
    try {
      const { message: ok } = await joinWaitlist(slug, {
        fullName: fullName.trim(),
        email: email.trim(),
        mobile: mobile || undefined,
        seatsRequested,
      });
      setMessage(ok);
      setJoined(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not join the waitlist.");
    } finally {
      setLoading(false);
    }
  }

  if (joined) {
    return (
      <div className="rounded-2xl border border-border bg-white p-6" id="waitlist">
        <h2 className="font-display text-xl font-semibold text-charcoal">You are on the waitlist</h2>
        <p className="mt-2 text-sm text-charcoal-muted">{message}</p>
      </div>
    );
  }

  return (
    <form
      id="waitlist"
      noValidate
      onSubmit={(e) => void handleSubmit(e)}
      className="rounded-2xl border border-border bg-white p-6"
    >
      <h2 className="font-display text-xl font-semibold text-charcoal">Join the waitlist</h2>
      <p className="mt-1 text-sm text-charcoal-muted">
        {destination} is full. We will email you if a seat opens so you can book.
      </p>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Label htmlFor="waitlist-name">Full name *</Label>
          <Input
            id="waitlist-name"
            value={fullName}
            autoComplete="name"
            aria-invalid={Boolean(errors.fullName)}
            error={errors.fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="waitlist-email">Email *</Label>
          <Input
            id="waitlist-email"
            type="email"
            value={email}
            autoComplete="email"
            aria-invalid={Boolean(errors.email)}
            error={errors.email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="waitlist-mobile">Mobile</Label>
          <Input
            id="waitlist-mobile"
            type="tel"
            inputMode="numeric"
            maxLength={10}
            value={mobile}
            autoComplete="tel"
            aria-invalid={Boolean(errors.mobile)}
            error={errors.mobile}
            onChange={(e) => setMobile(e.target.value.replace(/\D/g, ""))}
          />
        </div>
        <div>
          <Label htmlFor="waitlist-seats">Seats needed</Label>
          <Select
            id="waitlist-seats"
            value={String(seatsRequested)}
            onChange={(e) => setSeatsRequested(Number(e.target.value))}
          >
            {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </Select>
        </div>
      </div>
      {error && (
        <p className="mt-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
      <Button type="submit" className="mt-4" disabled={loading}>
        {loading ? "Joining..." : "Join waitlist"}
      </Button>
    </form>
  );
}
