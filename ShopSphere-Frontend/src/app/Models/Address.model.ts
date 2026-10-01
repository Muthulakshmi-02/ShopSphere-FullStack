export interface AddressResponse {
  addressId: number;
  label: string;
  shippingAddress: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  isDefault: boolean;
}

export interface AddressRequest {
  label: string;
  shippingAddress: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  isDefault: boolean;
}
