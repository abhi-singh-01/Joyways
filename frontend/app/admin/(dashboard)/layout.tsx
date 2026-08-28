import { AdminAuthGuard } from "@/components/admin/AdminAuthGuard";
import { AdminHeader } from "@/components/admin/AdminHeader";
import { AdminSidebar } from "@/components/admin/AdminSidebar";

export default function AdminDashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminAuthGuard>
      <div className="flex min-h-screen">
        <div className="fixed inset-y-0 hidden w-64 md:block">
          <AdminSidebar />
        </div>
        <div className="flex flex-1 flex-col md:ml-64">
          <AdminHeader />
          <main className="flex-1 p-6 sm:p-8">{children}</main>
        </div>
      </div>
    </AdminAuthGuard>
  );
}
