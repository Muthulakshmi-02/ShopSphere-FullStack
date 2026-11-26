import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { AddressRequest, AddressResponse } from '../Models/Address.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private http = inject(HttpClient);
  private readonly API_URL = `${API_BASE_URL}/api/addresses`;

  getMyAddresses(): Observable<ApiResponse<AddressResponse[]>> {
    return this.http.get<ApiResponse<AddressResponse[]>>(this.API_URL);
  }

  createAddress(data: AddressRequest): Observable<ApiResponse<AddressResponse>> {
    return this.http.post<ApiResponse<AddressResponse>>(this.API_URL, data);
  }

  updateAddress(addressId: number, data: AddressRequest): Observable<ApiResponse<AddressResponse>> {
    return this.http.put<ApiResponse<AddressResponse>>(`${this.API_URL}/${addressId}`, data);
  }

  deleteAddress(addressId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${addressId}`);
  }

  setDefault(addressId: number): Observable<ApiResponse<AddressResponse>> {
    return this.http.put<ApiResponse<AddressResponse>>(`${this.API_URL}/${addressId}/default`, {});
  }
}
