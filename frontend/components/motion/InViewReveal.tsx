"use client";

import { motion, type HTMLMotionProps } from "framer-motion";

type InViewRevealProps = HTMLMotionProps<"div"> & {
  delay?: number;
  duration?: number;
  y?: number;
};

/** Small client island for scroll-triggered entrance animations. */
export function InViewReveal({
  children,
  className,
  delay = 0,
  duration = 0.5,
  y = 20,
  ...props
}: Readonly<InViewRevealProps>) {
  return (
    <motion.div
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px 0px" }}
      transition={{ duration, delay }}
      className={className}
      {...props}
    >
      {children}
    </motion.div>
  );
}
