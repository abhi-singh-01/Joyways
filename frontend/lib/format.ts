export function formatPrice(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export function formatDateRange(start: string, end: string) {
  return `${formatDate(start)} – ${formatDate(end)}`;
}

/** Split multiline API text into non-empty lines */
export function splitLines(text: string | null | undefined): string[] {
  if (!text) return [];
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

/** Split admin pickup text (comma or newline) into unique choices. */
export function parsePickupPoints(text: string | null | undefined): string[] {
  if (!text) return [];
  const seen = new Set<string>();
  const points: string[] = [];
  for (const part of text.split(/[,\r\n]+/)) {
    const point = part.trim();
    if (!point || seen.has(point)) continue;
    seen.add(point);
    points.push(point);
  }
  return points;
}
