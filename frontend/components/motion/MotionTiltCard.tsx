"use client";

import { motion } from "framer-motion";

type MotionTiltCardProps = {
  children: React.ReactNode;
  className?: string;
  rotate?: number;
  delay?: number;
};

/** Client island for tilted destination cards with hover reset. */
export function MotionTiltCard({
  children,
  className,
  rotate = 0,
  delay = 0,
}: MotionTiltCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5, delay }}
      whileHover={{ y: -10, rotate: 0, scale: 1.03 }}
      style={{ rotate: `${rotate}deg` }}
      className={className}
    >
      {children}
    </motion.div>
  );
}
