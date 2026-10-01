import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { AddressRequest, AddressResponse } from '../Models/Address.model';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/addresses';

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
