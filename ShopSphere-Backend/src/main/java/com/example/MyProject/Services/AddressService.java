package com.example.MyProject.Services;

import com.example.MyProject.Address.dto.AddressRequest;
import com.example.MyProject.Address.dto.AddressResponse;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Address;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.AddressRepository;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES_PER_USER = 10;

    public ApiResponse<List<AddressResponse>> getMyAddresses() {
        User user = getAuthenticatedUser();
        List<AddressResponse> addresses = addressRepository
                .findByUser_UserIdOrderByIsDefaultDescAddressIdDesc(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<AddressResponse>>builder().success(true).data(addresses).build();
    }

    @Transactional
    public ApiResponse<AddressResponse> createAddress(AddressRequest request) {
        User user = getAuthenticatedUser();

        long existingCount = addressRepository.countByUser_UserId(user.getUserId());
        if (existingCount >= MAX_ADDRESSES_PER_USER) {
            throw new CartBusinessException(
                    "You've reached the limit of " + MAX_ADDRESSES_PER_USER + " saved addresses. Delete one first.");
        }

        // First address a user saves becomes their default automatically,
        // regardless of what they picked, so there's never a state with
        // saved addresses but no default.
        boolean shouldBeDefault = request.isDefault() || existingCount == 0;

        if (shouldBeDefault) {
            clearExistingDefault(user.getUserId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .shippingAddress(request.getShippingAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .phoneNumber(request.getPhoneNumber())
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        return ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Address saved")
                .data(mapToResponse(saved))
                .build();
    }

    @Transactional
    public ApiResponse<AddressResponse> updateAddress(Long addressId, AddressRequest request) {
        User user = getAuthenticatedUser();
        Address address = getOwnedAddressOrThrow(addressId, user.getUserId());

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(user.getUserId());
        }

        address.setLabel(request.getLabel());
        address.setShippingAddress(request.getShippingAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setPhoneNumber(request.getPhoneNumber());
        if (request.isDefault()) {
            address.setDefault(true);
        }

        Address saved = addressRepository.save(address);
        return ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Address updated")
                .data(mapToResponse(saved))
                .build();
    }

    @Transactional
    public ApiResponse<Void> deleteAddress(Long addressId) {
        User user = getAuthenticatedUser();
        Address address = getOwnedAddressOrThrow(addressId, user.getUserId());
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        // If the deleted address was the default, promote whichever address
        // is now "first" (most recently added) to default, so the user
        // still has exactly one default whenever they have any addresses.
        if (wasDefault) {
            List<Address> remaining = addressRepository
                    .findByUser_UserIdOrderByIsDefaultDescAddressIdDesc(user.getUserId());
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }

        return ApiResponse.<Void>builder().success(true).message("Address deleted").build();
    }

    @Transactional
    public ApiResponse<AddressResponse> setDefault(Long addressId) {
        User user = getAuthenticatedUser();
        Address address = getOwnedAddressOrThrow(addressId, user.getUserId());

        clearExistingDefault(user.getUserId());
        address.setDefault(true);
        Address saved = addressRepository.save(address);

        return ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Default address updated")
                .data(mapToResponse(saved))
                .build();
    }

    // --- Helpers ---

    private Address getOwnedAddressOrThrow(Long addressId, Long userId) {
        // IDOR guard, same pattern as the payment-verify fix: look the
        // address up scoped to the caller's own user ID, not just by its
        // raw ID, so one user can never edit/delete another user's address.
        return addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUser_UserIdAndIsDefaultTrue(userId).ifPresent(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .label(address.getLabel())
                .shippingAddress(address.getShippingAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .phoneNumber(address.getPhoneNumber())
                .isDefault(address.isDefault())
                .build();
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        String email = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
