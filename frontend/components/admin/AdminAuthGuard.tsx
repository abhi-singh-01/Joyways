"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getAdminSession, type AdminProfile } from "@/lib/api/authApi";
import { LoadingState } from "@/components/ui/LoadingState";

export function AdminAuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [admin, setAdmin] = useState<AdminProfile | null | undefined>(undefined);

  useEffect(() => {
    getAdminSession()
      .then((session) => {
        if (!session) {
          router.replace("/admin/login");
        } else {
          setAdmin(session);
        }
      })
      .catch(() => {
        router.replace("/admin/login");
      });
  }, [router]);

  if (admin === undefined) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface">
        <LoadingState message="Verifying session..." />
      </div>
    );
  }

  if (!admin) {
    return null;
  }

  return <>{children}</>;
}
