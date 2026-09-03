"use client";

import { motion, type HTMLMotionProps } from "framer-motion";

type MotionHoverLiftProps = HTMLMotionProps<"div"> & {
  lift?: number;
  scale?: number;
};

/** Small client island for hover lift/scale on otherwise static cards. */
export function MotionHoverLift({
  children,
  className,
  lift = -6,
  scale,
  ...props
}: MotionHoverLiftProps) {
  return (
    <motion.div
      whileHover={{ y: lift, ...(scale != null ? { scale } : {}) }}
      transition={{ type: "spring", stiffness: 320, damping: 22 }}
      className={className}
      {...props}
    >
      {children}
    </motion.div>
  );
}
