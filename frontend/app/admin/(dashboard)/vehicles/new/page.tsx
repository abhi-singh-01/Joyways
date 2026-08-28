import { VehicleForm } from "@/components/admin/VehicleForm";

export default function NewVehiclePage() {
  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Add Vehicle</h1>
      <p className="mt-1 text-charcoal-muted">Register a new vehicle in the fleet</p>
      <div className="mt-8">
        <VehicleForm />
      </div>
    </div>
  );
}
