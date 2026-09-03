"use client";

import Image from "next/image";
import { motion, useScroll, useTransform, type MotionValue } from "framer-motion";

type HeroLandscapeProps = {
  scrollYProgress: MotionValue<number>;
};

export function HeroLandscape({ scrollYProgress }: HeroLandscapeProps) {
  const scale = useTransform(scrollYProgress, [0, 1], [1, 1.18]);
  const y = useTransform(scrollYProgress, [0, 1], [0, 80]);

  return (
    <div className="absolute inset-0 -z-10 overflow-hidden bg-[#1b3a4a]">
      <motion.div className="absolute inset-0" style={{ scale, y }}>
        <Image
          src="/images/mountains-hero.webp"
          alt=""
          fill
          priority
          sizes="100vw"
          className="object-cover object-center"
        />
      </motion.div>
      <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-transparent to-black/40" />
    </div>
  );
}
