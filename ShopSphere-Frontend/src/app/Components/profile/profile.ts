import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ProfileService } from '../../Services/profile-service';
import { AddressService } from '../../Services/address-service';
import { AuthService } from '../../Services/auth-service';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';
import { UserProfileResponse } from '../../Models/User.model';
import { AddressRequest, AddressResponse } from '../../Models/Address.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatIconModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css'],
})
export class ProfilePage implements OnInit {
  private profileService = inject(ProfileService);
  private addressService = inject(AddressService);
  protected auth = inject(AuthService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);

  activeTab = signal<'profile' | 'addresses' | 'security'>('profile');

  // Profile data
  profile = signal<UserProfileResponse | null>(null);
  loadingProfile = signal(true);
  savingProfile = signal(false);
  editUsername = '';

  // Password change
  changingPassword = signal(false);
  showCurrentPassword = signal(false);
  showNewPassword = signal(false);
  passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  };

  // Address book
  addresses = signal<AddressResponse[]>([]);
  loadingAddresses = signal(false);
  isAddressModalOpen = signal(false);
  isEditingAddress = signal(false);
  editingAddressId = signal<number | null>(null);
  savingAddress = signal(false);
  settingDefaultId = signal<number | null>(null);
  deletingAddressId = signal<number | null>(null);

  addressForm: AddressRequest = this.initAddressForm();

  ngOnInit(): void {
    this.loadProfile();
    this.loadAddresses();
  }

  loadProfile(): void {
    this.loadingProfile.set(true);
    this.profileService.getProfile().subscribe({
      next: (res) => {
        if (res.data) {
          this.profile.set(res.data);
          this.editUsername = res.data.userName;
        }
        this.loadingProfile.set(false);
      },
      error: () => {
        this.loadingProfile.set(false);
        this.toastService.error('Failed to load profile details.');
      },
    });
  }

  loadAddresses(): void {
    this.loadingAddresses.set(true);
    this.addressService.getMyAddresses().subscribe({
      next: (res) => {
        this.addresses.set(res.data || []);
        this.loadingAddresses.set(false);
      },
      error: () => {
        this.loadingAddresses.set(false);
        this.toastService.error('Failed to load saved addresses.');
      },
    });
  }

  saveProfileUsername(): void {
    const trimmed = this.editUsername.trim();
    if (!trimmed || trimmed.length < 3) {
      this.toastService.error('Username must be at least 3 characters long.');
      return;
    }

    if (this.profile() && trimmed === this.profile()!.userName) {
      this.toastService.info('No changes made to username.');
      return;
    }

    this.savingProfile.set(true);
    this.profileService.updateProfile({ userName: trimmed }).subscribe({
      next: (res) => {
        this.savingProfile.set(false);
        if (res.data) {
          this.profile.set(res.data);
          this.editUsername = res.data.userName;
          if (this.auth.currentUser()) {
            const current = this.auth.currentUser()!;
            const updated = { ...current, userName: res.data.userName };
            this.auth.currentUser.set(updated);
            sessionStorage.setItem('user', JSON.stringify(updated));
          }
          this.toastService.success('Profile username updated successfully.');
        }
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.toastService.error(err.error?.message || 'Could not update username.');
      },
    });
  }

  submitPasswordChange(): void {
    const { currentPassword, newPassword, confirmPassword } = this.passwordForm;
    if (!currentPassword) {
      this.toastService.error('Current password is required.');
      return;
    }
    if (!newPassword || newPassword.length < 8) {
      this.toastService.error('New password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      this.toastService.error('New password and confirm password do not match.');
      return;
    }
    if (currentPassword === newPassword) {
      this.toastService.error('New password must be different from current password.');
      return;
    }

    this.changingPassword.set(true);
    this.profileService.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.toastService.success('Your password has been changed successfully.');
      },
      error: (err) => {
        this.changingPassword.set(false);
        this.toastService.error(err.error?.message || 'Could not change password.');
      },
    });
  }

  openAddAddressModal(): void {
    this.isEditingAddress.set(false);
    this.editingAddressId.set(null);
    this.addressForm = this.initAddressForm();
    this.isAddressModalOpen.set(true);
  }

  openEditAddressModal(addr: AddressResponse): void {
    this.isEditingAddress.set(true);
    this.editingAddressId.set(addr.addressId);
    this.addressForm = {
      label: addr.label || 'Home',
      shippingAddress: addr.shippingAddress,
      city: addr.city,
      state: addr.state,
      zipCode: addr.zipCode,
      phoneNumber: addr.phoneNumber,
      isDefault: addr.isDefault,
    };
    this.isAddressModalOpen.set(true);
  }

  closeAddressModal(): void {
    this.isAddressModalOpen.set(false);
  }

  saveAddress(): void {
    const f = this.addressForm;
    if (!f.shippingAddress?.trim()) {
      this.toastService.error('Street address is required.');
      return;
    }
    if (!f.city?.trim() || !f.state?.trim()) {
      this.toastService.error('City and State are required.');
      return;
    }
    if (!f.zipCode?.trim() || !/^\d{5,6}$/.test(f.zipCode.trim())) {
      this.toastService.error('Please enter a valid 5 or 6 digit postal PIN code.');
      return;
    }
    if (!f.phoneNumber?.trim() || f.phoneNumber.trim().length < 10) {
      this.toastService.error('Please enter a valid 10-digit phone number.');
      return;
    }

    this.savingAddress.set(true);
    const request = this.isEditingAddress()
      ? this.addressService.updateAddress(this.editingAddressId()!, f)
      : this.addressService.createAddress(f);

    request.subscribe({
      next: () => {
        this.savingAddress.set(false);
        this.isAddressModalOpen.set(false);
        this.toastService.success(this.isEditingAddress() ? 'Address updated.' : 'New address added.');
        this.loadAddresses();
        this.loadProfile();
      },
      error: (err) => {
        this.savingAddress.set(false);
        this.toastService.error(err.error?.message || 'Failed to save address.');
      },
    });
  }

  async setDefaultAddress(addr: AddressResponse): Promise<void> {
    if (addr.isDefault) return;
    this.settingDefaultId.set(addr.addressId);
    this.addressService.setDefault(addr.addressId).subscribe({
      next: () => {
        this.settingDefaultId.set(null);
        this.toastService.success(`Default delivery address set to "${addr.label || 'Selected'}".`);
        this.loadAddresses();
        this.loadProfile();
      },
      error: (err) => {
        this.settingDefaultId.set(null);
        this.toastService.error(err.error?.message || 'Could not set default address.');
      },
    });
  }

  async deleteAddress(addr: AddressResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Delete address "${addr.label || 'Saved Address'}"?`,
      message: `Are you sure you want to remove this address (${addr.city}, ${addr.zipCode})? This action cannot be undone.`,
      confirmLabel: 'Delete Address',
      danger: true,
    });
    if (!confirmed) return;

    this.deletingAddressId.set(addr.addressId);
    this.addressService.deleteAddress(addr.addressId).subscribe({
      next: () => {
        this.deletingAddressId.set(null);
        this.toastService.success('Address deleted successfully.');
        this.loadAddresses();
        this.loadProfile();
      },
      error: (err) => {
        this.deletingAddressId.set(null);
        this.toastService.error(err.error?.message || 'Could not delete address.');
      },
    });
  }

  private initAddressForm(): AddressRequest {
    return {
      label: 'Home',
      shippingAddress: '',
      city: '',
      state: '',
      zipCode: '',
      phoneNumber: '',
      isDefault: false,
    };
  }
}
