import { adminFetch } from "./client";
import type { PageResponse } from "./adminVehiclesApi";

export type CouponSummary = {
  id: number;
  code: string;
  packageId: number;
  packageTitle: string;
  destination: string;
  discountType: "PERCENT" | "FIXED";
  discountValue: number;
  expiresOn: string;
  active: boolean;
  expired: boolean;
  redemptionCount: number;
  createdAt: string;
};

export type CouponDetail = CouponSummary & {
  updatedAt: string;
};

export type CreateCouponPayload = {
  code: string;
  packageId: number;
  discountType: "PERCENT" | "FIXED";
  discountValue: number;
  expiresOn: string;
};

export type UpdateCouponPayload = {
  packageId: number;
  discountType: "PERCENT" | "FIXED";
  discountValue: number;
  expiresOn: string;
  active: boolean;
};

export async function listCoupons(params?: {
  query?: string;
  active?: boolean;
  packageId?: number;
  page?: number;
  size?: number;
}): Promise<PageResponse<CouponSummary>> {
  const search = new URLSearchParams();
  if (params?.query) search.set("query", params.query);
  if (params?.active != null) search.set("active", String(params.active));
  if (params?.packageId != null) search.set("packageId", String(params.packageId));
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 20));
  const response = await adminFetch<PageResponse<CouponSummary>>(
    `/api/admin/coupons?${search.toString()}`
  );
  return response.data ?? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

export async function getCoupon(id: number): Promise<CouponDetail> {
  const response = await adminFetch<CouponDetail>(`/api/admin/coupons/${id}`);
  if (!response.data) throw new Error("Coupon not found");
  return response.data;
}

export async function createCoupon(payload: CreateCouponPayload): Promise<CouponDetail> {
  const response = await adminFetch<CouponDetail>("/api/admin/coupons", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to create coupon");
  return response.data;
}

export async function updateCoupon(
  id: number,
  payload: UpdateCouponPayload
): Promise<CouponDetail> {
  const response = await adminFetch<CouponDetail>(`/api/admin/coupons/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!response.data) throw new Error("Failed to update coupon");
  return response.data;
}
