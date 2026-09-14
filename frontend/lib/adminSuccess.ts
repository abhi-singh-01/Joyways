export const ADMIN_SUCCESS_MESSAGES = {
  "package-created": "Package created successfully",
  "package-updated": "Package updated successfully",
  "package-status": "Package status updated successfully",
  "trip-created": "Trip created successfully",
  "vehicle-created": "Vehicle created successfully",
  "vehicle-updated": "Vehicle updated successfully",
  "driver-created": "Driver created successfully",
  "driver-updated": "Driver updated successfully",
  "coupon-created": "Coupon created successfully",
  "coupon-updated": "Coupon updated successfully",
  "booking-created": "Booking created successfully",
} as const;

export type AdminSuccessKey = keyof typeof ADMIN_SUCCESS_MESSAGES;

export function withSuccess(path: string, key: AdminSuccessKey): string {
  const separator = path.includes("?") ? "&" : "?";
  return `${path}${separator}success=${encodeURIComponent(key)}`;
}

export function adminSuccessMessage(key: string | null | undefined): string | null {
  if (!key) return null;
  if (key in ADMIN_SUCCESS_MESSAGES) {
    return ADMIN_SUCCESS_MESSAGES[key as AdminSuccessKey];
  }
  return "Saved successfully";
}
