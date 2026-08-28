import { PackageForm } from "@/components/admin/PackageForm";

export default function NewPackagePage() {
  return (
    <div>
      <h1 className="mb-6 font-display text-2xl font-semibold text-charcoal">
        Create Package
      </h1>
      <PackageForm mode="create" />
    </div>
  );
}
