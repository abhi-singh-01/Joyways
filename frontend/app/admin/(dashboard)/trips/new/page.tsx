import { TripForm } from "@/components/admin/TripForm";

export default function NewTripPage() {
  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Create Trip</h1>
      <p className="mt-1 text-charcoal-muted">Plan an operational trip for confirmed bookings</p>
      <div className="mt-8">
        <TripForm />
      </div>
    </div>
  );
}
