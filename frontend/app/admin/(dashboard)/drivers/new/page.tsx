import { DriverForm } from "@/components/admin/DriverForm";

export default function NewDriverPage() {
  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Add Driver</h1>
      <p className="mt-1 text-charcoal-muted">Register a new driver for trip operations</p>
      <div className="mt-8">
        <DriverForm />
      </div>
    </div>
  );
}
