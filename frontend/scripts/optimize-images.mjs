import sharp from "sharp";
import { mkdir, readdir } from "node:fs/promises";
import path from "node:path";

const publicDir = path.join(process.cwd(), "public");
const imagesDir = path.join(publicDir, "images");

const targets = [
  { input: path.join(imagesDir, "mountains-hero.png"), maxWidth: 1920, quality: 78 },
  { input: path.join(imagesDir, "mountains-peaks.png"), maxWidth: 960, quality: 78 },
  { input: path.join(imagesDir, "mountains-lake.png"), maxWidth: 960, quality: 78 },
  { input: path.join(imagesDir, "mountains-road.png"), maxWidth: 960, quality: 78 },
  { input: path.join(publicDir, "web-log.png"), maxWidth: 128, quality: 82 },
  { input: path.join(publicDir, "logo.png"), maxWidth: 480, quality: 82 },
];

await mkdir(imagesDir, { recursive: true });

for (const { input, maxWidth, quality } of targets) {
  const output = input.replace(/\.png$/i, ".webp");
  await sharp(input)
    .rotate()
    .resize({ width: maxWidth, withoutEnlargement: true })
    .webp({ quality, effort: 4 })
    .toFile(output);

  const before = (await import("node:fs/promises")).stat(input);
  const after = (await import("node:fs/promises")).stat(output);
  console.log(
    `${path.basename(input)} → ${path.basename(output)} (${Math.round(before.size / 1024)}KB → ${Math.round(after.size / 1024)}KB)`
  );
}

console.log("Done.");
