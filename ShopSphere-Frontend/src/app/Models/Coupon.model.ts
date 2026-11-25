export interface CouponResponse {
  id: number;
  code: string;
  type: 'percent' | 'flat';
  value: number;
  displayValue: number;
  active: boolean;
  minOrderAmount?: number | null;
  expiresAt?: string | null;
  expired: boolean;
  usageCount: number;
}

export interface CouponRequest {
  code: string;
  type: 'percent' | 'flat';
  value: number;
  active: boolean;
  minOrderAmount?: number | null;
  expiresAt?: string | null;
}
