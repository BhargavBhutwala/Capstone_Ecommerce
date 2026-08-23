package com.ebookstore.address.service;

import com.ebookstore.address.dto.AddressRequest;
import com.ebookstore.address.dto.AddressResponse;
import com.ebookstore.address.entity.Address;
import com.ebookstore.address.repository.AddressRepository;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AddressService}.
 * No Spring context — all dependencies are mocked.
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, userRepository);
    }

    // =========================================================================
    // listAddresses
    // =========================================================================

    @Test
    void listAddresses_returnsAllAddressesForUser() {
        Address a1 = buildAddress(1L, "Home");
        Address a2 = buildAddress(2L, "Work");
        when(addressRepository.findByUserId(10L)).thenReturn(List.of(a1, a2));

        List<AddressResponse> result = addressService.listAddresses(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getLabel()).isEqualTo("Home");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getLabel()).isEqualTo("Work");
    }

    @Test
    void listAddresses_returnsEmptyList_whenUserHasNoAddresses() {
        when(addressRepository.findByUserId(10L)).thenReturn(List.of());

        List<AddressResponse> result = addressService.listAddresses(10L);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // createAddress
    // =========================================================================

    @Test
    void createAddress_savesAndReturnsAddressResponse() {
        User userRef = new User();
        userRef.setId(10L);
        when(userRepository.getReferenceById(10L)).thenReturn(userRef);

        Address saved = buildAddress(5L, "Office");
        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressResponse result = addressService.createAddress(10L, buildRequest("Office"));

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getLabel()).isEqualTo("Office");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void createAddress_setsAllRequestFieldsOnEntity() {
        User userRef = new User();
        userRef.setId(10L);
        when(userRepository.getReferenceById(10L)).thenReturn(userRef);

        Address saved = buildAddress(5L, "Home");
        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressRequest req = buildRequest("Home");
        addressService.createAddress(10L, req);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address captured = captor.getValue();

        assertThat(captured.getLabel()).isEqualTo("Home");
        assertThat(captured.getAddressLine1()).isEqualTo("123 Main St");
        assertThat(captured.getCity()).isEqualTo("Springfield");
        assertThat(captured.getState()).isEqualTo("IL");
        assertThat(captured.getPostalCode()).isEqualTo("62701");
        assertThat(captured.getCountry()).isEqualTo("US");
    }

    // =========================================================================
    // updateAddress
    // =========================================================================

    @Test
    void updateAddress_updatesAndReturnsAddressResponse() {
        Address existing = buildAddress(5L, "Old Label");
        when(addressRepository.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenReturn(existing);

        AddressRequest req = buildRequest("New Label");
        AddressResponse result = addressService.updateAddress(10L, 5L, req);

        assertThat(result.getLabel()).isEqualTo("New Label");
        verify(addressRepository).save(existing);
    }

    @Test
    void updateAddress_throwsResourceNotFoundException_whenAddressNotFound() {
        when(addressRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(10L, 99L, buildRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // deleteAddress
    // =========================================================================

    @Test
    void deleteAddress_deletesAddress_whenFound() {
        Address existing = buildAddress(5L, "Home");
        when(addressRepository.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(existing));

        addressService.deleteAddress(10L, 5L);

        verify(addressRepository).delete(existing);
    }

    @Test
    void deleteAddress_throwsResourceNotFoundException_whenAddressNotFound() {
        when(addressRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.deleteAddress(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Address buildAddress(Long id, String label) {
        Address a = new Address();
        a.setId(id);
        a.setLabel(label);
        a.setAddressLine1("123 Main St");
        a.setAddressLine2(null);
        a.setCity("Springfield");
        a.setState("IL");
        a.setPostalCode("62701");
        a.setCountry("US");
        a.setDefault(false);
        return a;
    }

    private AddressRequest buildRequest(String label) {
        AddressRequest req = new AddressRequest();
        req.setLabel(label);
        req.setAddressLine1("123 Main St");
        req.setAddressLine2(null);
        req.setCity("Springfield");
        req.setState("IL");
        req.setPostalCode("62701");
        req.setCountry("US");
        req.setDefault(false);
        return req;
    }
}
