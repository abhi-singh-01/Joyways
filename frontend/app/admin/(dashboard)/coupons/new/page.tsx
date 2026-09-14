import Link from "next/link";
import { CouponForm } from "@/components/admin/CouponForm";

export default function NewCouponPage() {
  return (
    <div>
      <Link href="/admin/coupons" className="text-sm font-medium text-forest hover:underline">
        ← Back to coupons
      </Link>
      <h1 className="mt-4 font-display text-3xl font-semibold text-charcoal">New coupon</h1>
      <p className="mt-1 text-sm text-charcoal-muted">
        Create a package-only offer code. Each guest can redeem it once.
      </p>
      <div className="mt-8">
        <CouponForm />
      </div>
    </div>
  );
}
