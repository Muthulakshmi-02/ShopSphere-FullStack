package com.example.MyProject.Controller;

import com.example.MyProject.Address.dto.AddressRequest;
import com.example.MyProject.Address.dto.AddressResponse;
import com.example.MyProject.Services.AddressService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @CrossOrigin removed: CORS is configured once in SecurityConfig.
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.createAddress(request));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.deleteAddress(addressId));
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefault(addressId));
    }
}