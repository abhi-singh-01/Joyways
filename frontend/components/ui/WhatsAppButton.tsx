"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { MessageCircle, X } from "lucide-react";
import {
  whatsappUrl,
  DEFAULT_WHATSAPP_MESSAGE,
} from "@/lib/publicContact";

export function WhatsAppButton() {
  const [visible, setVisible] = useState(false);
  const [tooltipOpen, setTooltipOpen] = useState(false);

  // Slide in after 2s
  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  // Auto-show tooltip briefly after appearing
  useEffect(() => {
    if (!visible) return;
    const t = setTimeout(() => setTooltipOpen(true), 400);
    const t2 = setTimeout(() => setTooltipOpen(false), 4500);
    return () => {
      clearTimeout(t);
      clearTimeout(t2);
    };
  }, [visible]);

  const url = whatsappUrl(DEFAULT_WHATSAPP_MESSAGE);

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0, scale: 0.6, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.6, y: 20 }}
          transition={{ type: "spring", stiffness: 280, damping: 22 }}
          className="fixed bottom-6 right-5 z-50 flex flex-col items-end gap-2 sm:bottom-8 sm:right-8"
          aria-label="Chat on WhatsApp"
        >
          {/* Tooltip bubble */}
          <AnimatePresence>
            {tooltipOpen && (
              <motion.div
                initial={{ opacity: 0, y: 6, scale: 0.92 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 6, scale: 0.92 }}
                transition={{ duration: 0.2 }}
                className="relative mr-1 max-w-[200px] rounded-2xl bg-white px-4 py-3 shadow-xl ring-1 ring-border"
              >
                <button
                  type="button"
                  onClick={() => setTooltipOpen(false)}
                  className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-charcoal-muted text-white"
                  aria-label="Close tooltip"
                >
                  <X size={10} />
                </button>
                <p className="text-xs font-semibold text-charcoal">
                  💬 Need help planning?
                </p>
                <p className="mt-0.5 text-xs text-charcoal-muted">
                  Chat with us on WhatsApp!
                </p>
                {/* Arrow */}
                <span className="absolute -bottom-1.5 right-6 h-3 w-3 rotate-45 bg-white ring-1 ring-border" />
              </motion.div>
            )}
          </AnimatePresence>

          {/* WhatsApp Button */}
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Chat on WhatsApp"
            className="group relative flex h-14 w-14 items-center justify-center rounded-full shadow-2xl transition-transform duration-200 hover:scale-110 active:scale-95"
            onClick={() => setTooltipOpen(false)}
          >
            {/* Pulse ring */}
            <span className="absolute inset-0 rounded-full bg-[#25D366] opacity-30 animate-ping" />
            <span className="relative flex h-14 w-14 items-center justify-center rounded-full bg-[#25D366]">
              {/* WhatsApp SVG icon */}
              <svg
                viewBox="0 0 24 24"
                fill="white"
                className="h-7 w-7"
                aria-hidden
              >
                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
              </svg>
            </span>
          </a>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
