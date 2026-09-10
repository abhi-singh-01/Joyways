import Link from "next/link";
import { cn } from "@/lib/utils";

type ButtonLinkProps = React.ComponentProps<typeof Link> & {
  variant?: "primary" | "secondary" | "outline" | "ghost";
  size?: "sm" | "md" | "lg";
};

const variantStyles = {
  primary:
    "bg-forest text-white hover:bg-forest-light shadow-sm hover:shadow-md",
  secondary:
    "bg-accent text-charcoal hover:bg-accent-light shadow-sm hover:shadow-md",
  outline:
    "border-2 border-forest text-forest hover:bg-forest hover:text-white",
  ghost: "text-charcoal hover:bg-surface",
};

const sizeStyles = {
  sm: "px-4 py-2 text-sm",
  md: "px-6 py-3 text-base",
  lg: "px-8 py-4 text-lg",
};

export function ButtonLink({
  className,
  variant = "primary",
  size = "md",
  children,
  ...props
}: Readonly<ButtonLinkProps>) {
  return (
    <Link
      className={cn(
        "inline-flex items-center justify-center rounded-full font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-forest focus-visible:ring-offset-2",
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      {...props}
    >
      {children}
    </Link>
  );
}
