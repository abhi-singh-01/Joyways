"use client";

import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import type { TravellerInput } from "@/lib/api/bookingApi";

const DOCUMENT_TYPES = [
  { value: "AADHAAR", label: "Aadhaar" },
  { value: "PASSPORT", label: "Passport" },
  { value: "DRIVING_LICENCE", label: "Driving Licence" },
  { value: "OTHER", label: "Other Government ID" },
];

type TravellerFormProps = {
  index: number;
  data: TravellerInput;
  documentFile: File | null;
  onChange: (data: TravellerInput) => void;
  onDocumentChange: (file: File | null) => void;
};

export function TravellerForm({
  index,
  data,
  documentFile,
  onChange,
  onDocumentChange,
}: TravellerFormProps) {
  function update(field: keyof TravellerInput, value: string) {
    onChange({ ...data, [field]: value });
  }

  return (
    <div className="rounded-2xl border border-border bg-white p-5 sm:p-6">
      <h3 className="font-display text-lg font-semibold text-charcoal">
        Traveller {index + 1}
      </h3>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Label htmlFor={`name-${index}`}>Full Name *</Label>
          <Input
            id={`name-${index}`}
            required
            value={data.fullName}
            onChange={(e) => update("fullName", e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor={`dob-${index}`}>Date of Birth *</Label>
          <Input
            id={`dob-${index}`}
            type="date"
            required
            value={data.dateOfBirth}
            onChange={(e) => update("dateOfBirth", e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor={`gender-${index}`}>Gender *</Label>
          <Select
            id={`gender-${index}`}
            required
            value={data.gender}
            onChange={(e) => update("gender", e.target.value)}
          >
            <option value="">Select</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
            <option value="Other">Other</option>
          </Select>
        </div>
        <div>
          <Label htmlFor={`mobile-${index}`}>Mobile *</Label>
          <Input
            id={`mobile-${index}`}
            type="tel"
            required
            maxLength={10}
            placeholder="10-digit mobile"
            value={data.mobile}
            onChange={(e) => update("mobile", e.target.value.replace(/\D/g, ""))}
          />
        </div>
        <div>
          <Label htmlFor={`email-${index}`}>Email *</Label>
          <Input
            id={`email-${index}`}
            type="email"
            required
            value={data.email}
            onChange={(e) => update("email", e.target.value)}
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor={`emergency-${index}`}>Emergency Contact *</Label>
          <Input
            id={`emergency-${index}`}
            required
            placeholder="Name and phone number"
            value={data.emergencyContact}
            onChange={(e) => update("emergencyContact", e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor={`docType-${index}`}>ID Document Type *</Label>
          <Select
            id={`docType-${index}`}
            required
            value={data.idDocumentType}
            onChange={(e) => update("idDocumentType", e.target.value)}
          >
            <option value="">Select</option>
            {DOCUMENT_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor={`docRef-${index}`}>ID Reference (last 4 digits) *</Label>
          <Input
            id={`docRef-${index}`}
            required
            maxLength={50}
            placeholder="e.g. XXXX1234"
            value={data.idDocumentReference}
            onChange={(e) => update("idDocumentReference", e.target.value)}
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor={`docFile-${index}`}>Upload ID Document *</Label>
          <input
            id={`docFile-${index}`}
            type="file"
            accept="image/jpeg,image/png,image/webp,application/pdf"
            required
            className="mt-1 block w-full text-sm text-charcoal-muted file:mr-4 file:rounded-lg file:border-0 file:bg-forest file:px-4 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-forest-light"
            onChange={(e) => onDocumentChange(e.target.files?.[0] ?? null)}
          />
          {documentFile && (
            <p className="mt-1 text-xs text-charcoal-muted">
              Selected: {documentFile.name}
            </p>
          )}
          <p className="mt-1 text-xs text-charcoal-muted">
            JPEG, PNG, WebP or PDF — max 5MB. Stored securely and never shown publicly.
          </p>
        </div>
      </div>
    </div>
  );
}
