import { notFound } from "next/navigation";
import { getPackageById } from "@/lib/api/packagesApi";
import { BookingFlow } from "@/components/booking/BookingFlow";

export const dynamic = "force-dynamic";

type PageProps = {
  params: Promise<{ packageId: string }>;
};

export default async function BookPage({ params }: PageProps) {
  const { packageId } = await params;
  const id = Number(packageId);

  if (Number.isNaN(id)) {
    notFound();
  }

  let pkg;
  try {
    pkg = await getPackageById(id);
  } catch {
    notFound();
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
      <BookingFlow pkg={pkg} />
    </div>
  );
}
