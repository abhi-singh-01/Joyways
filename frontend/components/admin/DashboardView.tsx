"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import type { LucideIcon } from "lucide-react";
import { formatPrice, formatDate } from "@/lib/format";
import type { DashboardMetrics } from "@/lib/api/adminDashboardApi";
import { StatusBadge } from "@/components/admin/StatusBadge";
import {
  AlertTriangle,
  Bus,
  Calendar,
  CheckCircle,
  Clock,
  IndianRupee,
  Package,
  Route,
  UserCircle,
  Users,
  XCircle,
} from "lucide-react";

type DashboardViewProps = {
  metrics: DashboardMetrics;
};

function StatCard({
  label,
  value,
  icon: Icon,
  index,
}: {
  label: string;
  value: string | number;
  icon: LucideIcon;
  index: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay: index * 0.05 }}
      className="rounded-2xl border border-border bg-white p-5 shadow-sm"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-charcoal-muted">
            {label}
          </p>
          <p className="mt-2 text-2xl font-semibold text-charcoal">{value}</p>
        </div>
        <div className="rounded-xl bg-surface p-2.5">
          <Icon size={20} className="text-forest" />
        </div>
      </div>
    </motion.div>
  );
}

export function DashboardView({ metrics }: DashboardViewProps) {
  const bookingStats = [
    { label: "Total Bookings", value: metrics.totalBookings, icon: Package },
    { label: "Confirmed", value: metrics.confirmedBookings, icon: CheckCircle },
    { label: "Payment Pending", value: metrics.pendingPayments, icon: Clock },
    { label: "Cancelled", value: metrics.cancelledBookings, icon: XCircle },
    {
      label: "Revenue",
      value: formatPrice(Number(metrics.revenue)),
      icon: IndianRupee,
    },
    { label: "Active Packages", value: metrics.activePackages, icon: Package },
    { label: "Package Upcoming", value: metrics.upcomingTrips, icon: Calendar },
    { label: "Available Seats", value: metrics.availableSeats, icon: Users },
    { label: "Paid, not confirmed", value: metrics.paidButNotConfirmed ?? 0, icon: AlertTriangle },
    { label: "Confirmed, not paid", value: metrics.confirmedButNotPaid ?? 0, icon: AlertTriangle },
  ];

  const operationsStats = [
    { label: "Today's Trips", value: metrics.todaysTrips, icon: Route },
    { label: "Upcoming Trips", value: metrics.upcomingOperationalTrips, icon: Calendar },
    { label: "Available Vehicles", value: metrics.availableVehicles, icon: Bus },
    { label: "Assigned Vehicles", value: metrics.assignedVehicles, icon: Bus },
    { label: "Available Drivers", value: metrics.availableDrivers, icon: UserCircle },
    { label: "Assigned Drivers", value: metrics.assignedDrivers, icon: UserCircle },
    { label: "In Progress", value: metrics.tripsInProgress, icon: Route },
    { label: "Completed Today", value: metrics.tripsCompletedToday, icon: CheckCircle },
  ];

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold text-charcoal sm:text-3xl">
        Dashboard
      </h1>
      <p className="mt-1 text-charcoal-muted">Overview of bookings and trip operations</p>

      {(metrics.paidButNotConfirmed > 0 || metrics.confirmedButNotPaid > 0) && (
        <div className="mt-6 rounded-2xl border border-amber-300 bg-amber-50 p-5">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 text-amber-700" size={20} />
            <div>
              <h2 className="text-lg font-semibold text-charcoal">Needs review</h2>
              <p className="mt-1 text-sm text-charcoal-muted">
                Booking status and payment status disagree. Confirm the true state in Razorpay
                before changing any booking.
              </p>
              <ul className="mt-3 space-y-1 text-sm text-charcoal">
                <li>
                  Paid but not confirmed: <strong>{metrics.paidButNotConfirmed}</strong>
                </li>
                <li>
                  Confirmed but not paid: <strong>{metrics.confirmedButNotPaid}</strong>
                </li>
              </ul>
              <Link href="/admin/bookings" className="mt-3 inline-block text-sm font-medium text-forest hover:underline">
                Open bookings
              </Link>
            </div>
          </div>
        </div>
      )}

      <h2 className="mt-8 text-lg font-semibold text-charcoal">Bookings & Packages</h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {bookingStats.map((stat, index) => (
          <StatCard key={stat.label} {...stat} index={index} />
        ))}
      </div>

      <h2 className="mt-10 text-lg font-semibold text-charcoal">Operations</h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {operationsStats.map((stat, index) => (
          <StatCard key={stat.label} {...stat} index={index} />
        ))}
      </div>

      <div className="mt-10 rounded-2xl border border-border bg-white p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-charcoal">Upcoming Trips</h2>
          <Link href="/admin/trips" className="text-sm text-forest hover:underline">
            View all
          </Link>
        </div>
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="text-charcoal-muted">
              <tr>
                <th className="px-2 py-2 font-medium">Reference</th>
                <th className="px-2 py-2 font-medium">Destination</th>
                <th className="px-2 py-2 font-medium">Date</th>
                <th className="px-2 py-2 font-medium">Travellers</th>
                <th className="px-2 py-2 font-medium">Vehicle</th>
                <th className="px-2 py-2 font-medium">Driver</th>
                <th className="px-2 py-2 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {metrics.upcomingTripList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-2 py-6 text-charcoal-muted">
                    No upcoming operational trips
                  </td>
                </tr>
              ) : (
                metrics.upcomingTripList.map((trip) => (
                  <tr key={trip.id} className="border-t border-border">
                    <td className="px-2 py-3">
                      <Link
                        href={`/admin/trips/${trip.id}`}
                        className="font-medium text-forest hover:underline"
                      >
                        {trip.tripReference}
                      </Link>
                    </td>
                    <td className="px-2 py-3">{trip.destination}</td>
                    <td className="px-2 py-3">{formatDate(trip.travelDate)}</td>
                    <td className="px-2 py-3">{trip.totalTravellers}</td>
                    <td className="px-2 py-3">{trip.vehicleRegistration ?? "—"}</td>
                    <td className="px-2 py-3">{trip.driverName ?? "—"}</td>
                    <td className="px-2 py-3">
                      <StatusBadge status={trip.status} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
