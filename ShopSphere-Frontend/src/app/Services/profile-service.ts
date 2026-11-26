import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ChangePasswordRequest, UpdateProfileRequest, UserProfileResponse } from '../Models/User.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/user/profile`;

  getProfile(): Observable<ApiResponse<UserProfileResponse>> {
    return this.http.get<ApiResponse<UserProfileResponse>>(this.apiUrl);
  }

  updateProfile(request: UpdateProfileRequest): Observable<ApiResponse<UserProfileResponse>> {
    return this.http.put<ApiResponse<UserProfileResponse>>(this.apiUrl, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/change-password`, request);
  }
}
