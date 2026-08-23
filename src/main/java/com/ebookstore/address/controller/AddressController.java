package com.ebookstore.address.controller;

import com.ebookstore.address.dto.AddressRequest;
import com.ebookstore.address.dto.AddressResponse;
import com.ebookstore.address.service.AddressService;
import com.ebookstore.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Address management endpoints.
 *
 * <p>The authenticated user's database id is obtained exclusively from
 * {@link AuthenticatedUser#getId()} — never from request parameters or body.
 */
@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /** operationId: listAddresses */
    @GetMapping
    public ResponseEntity<List<AddressResponse>> listAddresses(Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(addressService.listAddresses(userId));
    }

    /** operationId: createAddress */
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** operationId: updateAddress */
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        AddressResponse response = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    /** operationId: deleteAddress */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
