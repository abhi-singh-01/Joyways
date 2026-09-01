"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Textarea } from "@/components/ui/Textarea";
import { PUBLIC_EMAILS } from "@/lib/publicEmails";
import {
  contactEnquirySchema,
  fieldErrorsFromZod,
  type FieldErrors,
} from "@/lib/formValidation";

type ContactFormData = {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
};

const emptyForm: ContactFormData = {
  name: "",
  email: "",
  phone: "",
  subject: "",
  message: "",
};

export function ContactEnquiryForm() {
  const [form, setForm] = useState<ContactFormData>(emptyForm);
  const [errors, setErrors] = useState<FieldErrors<ContactFormData>>({});
  const [submitted, setSubmitted] = useState(false);

  function updateField<K extends keyof ContactFormData>(key: K, value: ContactFormData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const result = contactEnquirySchema.safeParse(form);

    if (!result.success) {
      setErrors(fieldErrorsFromZod(result));
      setSubmitted(false);
      document
        .querySelector<HTMLElement>("[aria-invalid='true']")
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }

    setErrors({});
    const data = result.data;
    const body = [
      `Name: ${data.name}`,
      `Email: ${data.email}`,
      `Phone: ${data.phone}`,
      "",
      data.message,
    ].join("\n");

    const mailto = `mailto:${PUBLIC_EMAILS.support}?subject=${encodeURIComponent(
      `[JOYWAY Enquiry] ${data.subject}`
    )}&body=${encodeURIComponent(body)}`;

    window.location.href = mailto;
    setSubmitted(true);
  }

  return (
    <div className="rounded-2xl border border-border bg-white p-6 sm:p-8">
      <h2 className="font-display text-2xl font-semibold text-charcoal">Send us a message</h2>
      <p className="mt-2 text-sm text-charcoal-muted">
        All fields marked * are required. Your email app will open to send the message to our
        support team.
      </p>

      {submitted && (
        <p className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">
          If your email app did not open, write to us at{" "}
          <a href={`mailto:${PUBLIC_EMAILS.support}`} className="font-medium underline">
            {PUBLIC_EMAILS.support}
          </a>
          .
        </p>
      )}

      <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Label htmlFor="contactName">Full Name *</Label>
            <Input
              id="contactName"
              required
              autoComplete="name"
              aria-invalid={Boolean(errors.name)}
              error={errors.name}
              value={form.name}
              onChange={(e) => updateField("name", e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="contactEmail">Email *</Label>
            <Input
              id="contactEmail"
              type="email"
              required
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              error={errors.email}
              value={form.email}
              onChange={(e) => updateField("email", e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="contactPhone">Mobile *</Label>
            <Input
              id="contactPhone"
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              required
              maxLength={10}
              aria-invalid={Boolean(errors.phone)}
              error={errors.phone}
              value={form.phone}
              onChange={(e) => updateField("phone", e.target.value.replace(/\D/g, ""))}
            />
          </div>
          <div className="sm:col-span-2">
            <Label htmlFor="contactSubject">Subject *</Label>
            <Input
              id="contactSubject"
              required
              aria-invalid={Boolean(errors.subject)}
              error={errors.subject}
              value={form.subject}
              onChange={(e) => updateField("subject", e.target.value)}
              placeholder="e.g. Group trip enquiry for Nainital"
            />
          </div>
          <div className="sm:col-span-2">
            <Label htmlFor="contactMessage">Message *</Label>
            <Textarea
              id="contactMessage"
              required
              rows={5}
              aria-invalid={Boolean(errors.message)}
              className={errors.message ? "border-red-400 focus:border-red-400 focus:ring-red-100" : undefined}
              value={form.message}
              onChange={(e) => updateField("message", e.target.value)}
              placeholder="Tell us about your trip dates, group size, or question..."
            />
            {errors.message && (
              <p className="mt-1 text-xs text-red-600">{errors.message}</p>
            )}
          </div>
        </div>

        <Button type="submit" size="lg">
          Send enquiry
        </Button>
      </form>
    </div>
  );
}
