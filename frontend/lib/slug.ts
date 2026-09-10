/** URL slug from a package title. Avoids backtracking regexes. */
export function toSlug(value: string): string {
  return value
    .toLowerCase()
    .trim()
    .replaceAll(/['"]/g, "")
    .split(/[^a-z0-9]+/)
    .filter(Boolean)
    .join("-");
}
