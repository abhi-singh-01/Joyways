import { cn } from "@/lib/utils";

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & {
  error?: string;
};

export function Select({ className, children, error, ...props }: SelectProps) {
  return (
    <div>
      <select
        className={cn(
          "w-full rounded-xl border border-border bg-white px-4 py-2.5 text-sm text-charcoal outline-none focus:border-forest focus:ring-2 focus:ring-forest/20",
          error && "border-red-400 focus:border-red-400 focus:ring-red-100",
          className
        )}
        {...props}
      >
        {children}
      </select>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
