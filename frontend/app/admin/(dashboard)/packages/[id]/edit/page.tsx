import { ErrorState } from "@/components/ui/ErrorState";
import { EditPackageContent } from "@/components/admin/EditPackageContent";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function EditPackagePage({ params }: Readonly<PageProps>) {
  const { id: idParam } = await params;
  const id = Number(idParam);

  if (Number.isNaN(id)) {
    return <ErrorState message="Invalid package ID" fullPage />;
  }

  return <EditPackageContent id={id} />;
}
