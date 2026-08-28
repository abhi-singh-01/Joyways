import { cn } from "@/lib/utils";

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement>;

export function Select({ className, children, ...props }: SelectProps) {
  return (
    <select
      className={cn(
        "w-full rounded-xl border border-border bg-white px-4 py-2.5 text-sm text-charcoal outline-none focus:border-forest focus:ring-2 focus:ring-forest/20",
        className
      )}
      {...props}
    >
      {children}
    </select>
  );
}
