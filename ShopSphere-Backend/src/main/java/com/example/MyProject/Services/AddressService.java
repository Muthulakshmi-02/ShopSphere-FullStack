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

    @Transactional(readOnly = true)
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

        // The first address a user saves is always their default, so there is never a state
        // with saved addresses but no default.
        boolean shouldBeDefault = request.isDefault() || existingCount == 0;
        if (shouldBeDefault) {
            addressRepository.clearDefaultForUser(user.getUserId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .zipCode(request.getZipCode().trim())
                .phoneNumber(request.getPhoneNumber().trim())
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
            addressRepository.clearDefaultForUser(user.getUserId());
        }

        address.setLabel(request.getLabel().trim());
        address.setShippingAddress(request.getShippingAddress().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setZipCode(request.getZipCode().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        // An address can become the default here, but it is never un-defaulted by an edit:
        // a user always keeps exactly one default.
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

        // If the default was deleted, promote the most recently added remaining address.
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

        addressRepository.clearDefaultForUser(user.getUserId());
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
        // IDOR guard: looked up scoped to the caller's own user id, never by raw id alone.
        return addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
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