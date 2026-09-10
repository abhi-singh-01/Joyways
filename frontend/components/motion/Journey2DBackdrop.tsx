/** CSS-only 2D travel illustration. Safe for SSR; animations pause under reduced motion. */
export function Journey2DBackdrop() {
  return (
    <svg
      className="pointer-events-none absolute right-0 top-0 h-36 w-44 overflow-visible sm:h-44 sm:w-56 md:inset-y-0 md:h-full md:w-[58%]"
      viewBox="0 0 720 420"
      fill="none"
      aria-hidden
      preserveAspectRatio="xMaxYMid slice"
    >
      <circle className="journey-sun" cx="560" cy="88" r="36" fill="#e8c9a8" />
      <circle className="journey-sun-glow" cx="560" cy="88" r="56" fill="#e8c9a8" opacity="0.28" />

      <path d="M40 420 L180 210 L260 270 L360 150 L460 230 L560 120 L720 260 V420 Z" fill="#1b4332" opacity="0.18" />
      <path d="M0 420 L140 250 L220 300 L330 180 L450 250 L580 160 L720 280 V420 Z" fill="#1b4332" opacity="0.28" />
      <path d="M0 420 L90 310 L190 340 L300 250 L420 320 L540 240 L720 330 V420 Z" fill="#2d6a4f" opacity="0.45" />

      <path
        className="journey-road"
        d="M80 420 C 180 360, 240 340, 310 300 S 470 250, 560 210 S 680 170, 720 150"
        stroke="#1a1a1a"
        strokeWidth="10"
        strokeLinecap="round"
        opacity="0.35"
      />
      <path
        className="journey-road-dash"
        d="M80 420 C 180 360, 240 340, 310 300 S 470 250, 560 210 S 680 170, 720 150"
        stroke="#faf9f7"
        strokeWidth="2"
        strokeDasharray="8 12"
        strokeLinecap="round"
        opacity="0.8"
      />

      <g className="journey-bus">
        <rect x="-18" y="-10" width="36" height="16" rx="4" fill="#1b4332" />
        <rect x="-10" y="-8" width="12" height="8" rx="1" fill="#e8c9a8" />
        <circle cx="-10" cy="7" r="3.5" fill="#1a1a1a" />
        <circle cx="10" cy="7" r="3.5" fill="#1a1a1a" />
      </g>

      <g className="journey-bird" stroke="#1b4332" strokeWidth="1.8" strokeLinecap="round" opacity="0.7">
        <path d="M500 130 q10 -12 20 0" />
        <path d="M528 142 q8 -10 16 0" />
        <path d="M512 152 q7 -8 14 0" />
      </g>
    </svg>
  );
}
