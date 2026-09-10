import { cn } from "@/lib/utils";

type LabelProps = React.LabelHTMLAttributes<HTMLLabelElement>;

export function Label({ className, children, ...props }: Readonly<LabelProps>) {
  return (
    <label
      className={cn("mb-1.5 block text-sm font-medium text-charcoal", className)}
      {...props}
    >
      {children}
    </label>
  );
}
