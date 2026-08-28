import { cn } from "@/lib/utils";

type TextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
  error?: string;
};

export function Textarea({ className, error, ...props }: TextareaProps) {
  return (
    <div>
      <textarea
        className={cn(
          "w-full rounded-xl border border-border bg-white px-4 py-2.5 text-sm text-charcoal outline-none transition-colors placeholder:text-charcoal-muted/60 focus:border-forest focus:ring-2 focus:ring-forest/20",
          error && "border-red-400",
          className
        )}
        rows={4}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
