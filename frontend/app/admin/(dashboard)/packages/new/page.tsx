import { PackageForm } from "@/components/admin/PackageForm";

export default function NewPackagePage() {
  return (
    <div>
      <h1 className="mb-2 font-display text-2xl font-semibold text-charcoal">
        Create Package
      </h1>
      <p className="mb-6 text-sm text-charcoal-muted">
        Enter the trip details first. Photos can be uploaded after you save.
      </p>
      <PackageForm mode="create" />
    </div>
  );
}
