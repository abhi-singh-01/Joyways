"use client";

import { useParams } from "next/navigation";
import { AdminTripDetailView } from "@/components/admin/AdminTripDetailView";

export default function AdminTripDetailPage() {
  const params = useParams<{ id: string }>();
  return <AdminTripDetailView tripId={Number(params.id)} />;
}
