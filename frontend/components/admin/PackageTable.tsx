"use client";

import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import { Plus, Pencil } from "lucide-react";
import { formatDate, formatPrice } from "@/lib/format";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import type { AdminPackageSummary } from "@/lib/api/adminPackagesApi";

type PackageTableProps = {
  packages: AdminPackageSummary[];
};

export function PackageTable({ packages }: PackageTableProps) {
  if (packages.length === 0) {
    return (
      <EmptyState
        title="No packages yet"
        message="Create your first travel package to get started."
        actionLabel="Create Package"
        actionHref="/admin/packages/new"
      />
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[800px] text-left text-sm">
          <thead>
            <tr className="border-b border-border bg-surface/50 text-xs uppercase tracking-wide text-charcoal-muted">
              <th className="px-4 py-3 font-medium">Package</th>
              <th className="px-4 py-3 font-medium">Date</th>
              <th className="px-4 py-3 font-medium">Price</th>
              <th className="px-4 py-3 font-medium">Seats</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {packages.map((pkg, index) => (
              <motion.tr
                key={pkg.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: index * 0.03 }}
                className="border-b border-border last:border-0 hover:bg-surface/30"
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="relative h-10 w-14 shrink-0 overflow-hidden rounded-lg bg-surface">
                      {pkg.heroImageUrl ? (
                        <Image
                          src={pkg.heroImageUrl}
                          alt={pkg.destination}
                          fill
                          className="object-cover"
                          sizes="56px"
                        />
                      ) : null}
                    </div>
                    <div>
                      <p className="font-medium text-charcoal">{pkg.title}</p>
                      <p className="text-xs text-charcoal-muted">{pkg.destination}</p>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3 text-charcoal-muted">
                  {formatDate(pkg.startDate)}
                </td>
                <td className="px-4 py-3 font-medium text-forest">
                  {formatPrice(pkg.pricePerPerson)}
                </td>
                <td className="px-4 py-3 text-charcoal-muted">
                  {pkg.availableSeats}/{pkg.totalSeats}
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={pkg.status} />
                </td>
                <td className="px-4 py-3">
                  <Link href={`/admin/packages/${pkg.id}/edit`}>
                    <Button variant="ghost" size="sm">
                      <Pencil size={14} className="mr-1" />
                      Edit
                    </Button>
                  </Link>
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function PackageListHeader() {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 className="font-display text-2xl font-semibold text-charcoal">Packages</h1>
        <p className="text-sm text-charcoal-muted">Manage travel packages</p>
      </div>
      <Link href="/admin/packages/new">
        <Button>
          <Plus size={16} className="mr-2" />
          Create Package
        </Button>
      </Link>
    </div>
  );
}
