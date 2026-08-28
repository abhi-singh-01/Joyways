/** Safe public contact addresses — never put SMTP credentials here. */
export const PUBLIC_EMAILS = {
  support: process.env.NEXT_PUBLIC_SUPPORT_EMAIL ?? "support@joyway.in",
  bookings: process.env.NEXT_PUBLIC_BOOKINGS_EMAIL ?? "bookings@joyway.in",
  general: process.env.NEXT_PUBLIC_GENERAL_EMAIL ?? "hello@joyway.in",
} as const;
