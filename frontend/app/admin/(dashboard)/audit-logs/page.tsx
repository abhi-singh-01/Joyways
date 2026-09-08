"use client";

import { useEffect, useState } from "react";
import { listAdminAuditLogs, type AdminAuditLog } from "@/lib/api/adminOpsLogsApi";
import { ErrorState } from "@/components/ui/ErrorState";
import { formatDate } from "@/lib/format";

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState<AdminAuditLog[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listAdminAuditLogs()
      .then((page) => setLogs(page.content))
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load audit logs"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-charcoal-muted">Loading audit logs...</p>;
  if (error) return <ErrorState title="Unable to load audit logs" message={error} />;

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Audit log</h1>
      <p className="mt-1 text-charcoal-muted">Admin actions on packages, trips, vehicles, and refunds</p>

      <div className="mt-8 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-surface text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">When</th>
              <th className="px-4 py-3 font-medium">Actor</th>
              <th className="px-4 py-3 font-medium">Action</th>
              <th className="px-4 py-3 font-medium">Summary</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-charcoal-muted">
                  No audit entries yet
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 whitespace-nowrap">{formatDate(log.createdAt)}</td>
                  <td className="px-4 py-3">
                    <p>{log.actorName}</p>
                    {log.actorEmail && (
                      <p className="text-xs text-charcoal-muted">{log.actorEmail}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 font-medium">{log.action}</td>
                  <td className="px-4 py-3">{log.summary}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
