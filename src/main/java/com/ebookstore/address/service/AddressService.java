package com.ebookstore.address.service;

import com.ebookstore.address.dto.AddressRequest;
import com.ebookstore.address.dto.AddressResponse;
import com.ebookstore.address.entity.Address;
import com.ebookstore.address.repository.AddressRepository;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for address CRUD operations.
 *
 * <p>Ownership is enforced at the repository level via
 * {@link AddressRepository#findByIdAndUserId(Long, Long)}, which returns empty
 * when the address does not belong to the requesting user — preventing
 * information leakage about the resource's existence.
 */
@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    /** operationId: listAddresses */
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(AddressService::toResponse)
                .toList();
    }

    /** operationId: createAddress */
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.getReferenceById(userId);
        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request);
        return toResponse(addressRepository.save(address));
    }

    /** operationId: updateAddress */
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
        applyRequest(address, request);
        return toResponse(addressRepository.save(address));
    }

    /** operationId: deleteAddress */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
        addressRepository.delete(address);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void applyRequest(Address address, AddressRequest req) {
        address.setLabel(req.getLabel());
        address.setAddressLine1(req.getAddressLine1());
        address.setAddressLine2(req.getAddressLine2());
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPostalCode(req.getPostalCode());
        address.setCountry(req.getCountry());
        address.setDefault(req.isDefault());
    }

    static AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getLabel(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountry(),
                a.isDefault());
    }
}
