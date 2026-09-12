"use client";

import Image from "next/image";
import { motion, useTransform, type MotionValue } from "framer-motion";
import { CINEMATIC_IMAGES } from "@/components/cinematic/cinematicMedia";

type HeroLandscapeProps = {
  scrollYProgress: MotionValue<number>;
};

/** Full-bleed hero; cap reported slot at 1920px on ultra-wide viewports. */
const heroImageSizes =
  "(max-width: 640px) 100vw, (max-width: 1920px) 100vw, 1920px";

export function HeroLandscape({ scrollYProgress }: Readonly<HeroLandscapeProps>) {
  const scale = useTransform(scrollYProgress, [0, 1], [1, 1.18]);
  const y = useTransform(scrollYProgress, [0, 1], [0, 80]);

  return (
    <div className="absolute inset-0 -z-10 overflow-hidden bg-[#1b3a4a]">
      <motion.div className="absolute inset-0" style={{ scale, y }}>
        <Image
          src={CINEMATIC_IMAGES.hero}
          alt=""
          fill
          priority
          sizes={heroImageSizes}
          className="object-cover object-center"
        />
      </motion.div>
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-transparent to-black/40" />
    </div>
  );
}
