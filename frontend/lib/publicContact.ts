import { PUBLIC_EMAILS } from "@/lib/publicEmails";

/** E.164 without + — override via env if needed */
export const PUBLIC_CALL_NUMBER =
  process.env.NEXT_PUBLIC_CALL_NUMBER?.replace(/\D/g, "") ?? "919696313547";

/** E.164 without + — override via env if needed */
export const PUBLIC_WHATSAPP_NUMBER =
  process.env.NEXT_PUBLIC_WHATSAPP_NUMBER?.replace(/\D/g, "") ?? "917050971956";

export function formatPhoneDisplay(e164WithoutPlus: string): string {
  const digits = e164WithoutPlus.replace(/\D/g, "");
  if (digits.startsWith("91") && digits.length === 12) {
    return digits.slice(2);
  }
  return digits;
}

export function telUrl(e164WithoutPlus: string): string {
  return `tel:+${e164WithoutPlus.replace(/\D/g, "")}`;
}

export function whatsappUrl(message: string): string {
  return `https://wa.me/${PUBLIC_WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`;
}

export const DEFAULT_WHATSAPP_MESSAGE =
  "Hi Joyway! I'd like help choosing a group trip from Delhi.";

export { PUBLIC_EMAILS };
