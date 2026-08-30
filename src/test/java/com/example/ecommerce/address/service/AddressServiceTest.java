package com.example.ecommerce.address.service;

import com.example.ecommerce.address.dto.AddressResponse;
import com.example.ecommerce.address.dto.CreateAddressRequest;
import com.example.ecommerce.address.entity.UserAddress;
import com.example.ecommerce.address.repository.AddressRepository;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AddressService.
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private UserAddress address;
    private CreateAddressRequest request;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        address = UserAddress.builder()
                .id(1L)
                .user(user)
                .fullName("John Doe")
                .phoneNumber("9876543210")
                .addressLine1("123 Main Street")
                .addressLine2("Near City Mall")
                .city("Brahmapur")
                .state("Odisha")
                .postalCode("760001")
                .country("India")
                .isDefault(false)
                .build();

        request = new CreateAddressRequest();

        request.setFullName("John Doe");
        request.setPhoneNumber("9876543210");
        request.setAddressLine1("123 Main Street");
        request.setAddressLine2("Near City Mall");
        request.setCity("Brahmapur");
        request.setState("Odisha");
        request.setPostalCode("760001");
        request.setCountry("India");
        request.setIsDefault(false);
    }

    // ---------------------------------------------------------
    // CREATE ADDRESS
    // ---------------------------------------------------------

    @Test
    void shouldCreateAddressSuccessfully() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.save(any(UserAddress.class)))
                .thenReturn(address);

        AddressResponse response = addressService.createAddress(
                "test@example.com",
                request);

        assertNotNull(response);

        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getFullName());
        assertEquals("9876543210", response.getPhoneNumber());
        assertEquals("123 Main Street", response.getAddressLine1());
        assertEquals("Near City Mall", response.getAddressLine2());
        assertEquals("Brahmapur", response.getCity());
        assertEquals("Odisha", response.getState());
        assertEquals("760001", response.getPostalCode());
        assertEquals("India", response.getCountry());
        assertFalse(response.getIsDefault());

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository)
                .save(any(UserAddress.class));
    }

    @Test
    void shouldCreateDefaultAddressSuccessfully() {

        request.setIsDefault(true);

        UserAddress oldAddress = UserAddress.builder()
                .id(2L)
                .user(user)
                .fullName("Old Address")
                .phoneNumber("9999999999")
                .addressLine1("Old Street")
                .city("Brahmapur")
                .state("Odisha")
                .postalCode("760002")
                .country("India")
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByUserId(1L))
                .thenReturn(List.of(oldAddress));

        when(addressRepository.save(any(UserAddress.class)))
                .thenReturn(address);

        AddressResponse response = addressService.createAddress(
                "test@example.com",
                request);

        assertNotNull(response);

        verify(addressRepository)
                .findByUserId(1L);

        verify(addressRepository)
                .save(any(UserAddress.class));

        assertFalse(oldAddress.getIsDefault());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringCreate() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.createAddress(
                        "test@example.com",
                        request));

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository, never())
                .save(any(UserAddress.class));
    }

    // ---------------------------------------------------------
    // GET ADDRESSES
    // ---------------------------------------------------------

    @Test
    void shouldGetUserAddressesSuccessfully() {

        UserAddress secondAddress = UserAddress.builder()
                .id(2L)
                .user(user)
                .fullName("John Doe")
                .phoneNumber("9876543210")
                .addressLine1("456 Market Road")
                .addressLine2("")
                .city("Brahmapur")
                .state("Odisha")
                .postalCode("760003")
                .country("India")
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByUserId(1L))
                .thenReturn(List.of(address, secondAddress));

        List<AddressResponse> response = addressService.getUserAddresses(
                "test@example.com");

        assertNotNull(response);

        assertEquals(2, response.size());

        assertEquals(
                "John Doe",
                response.get(0).getFullName());

        assertEquals(
                "123 Main Street",
                response.get(0).getAddressLine1());

        assertEquals(
                "456 Market Road",
                response.get(1).getAddressLine1());

        assertFalse(
                response.get(0).getIsDefault());

        assertTrue(
                response.get(1).getIsDefault());

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository)
                .findByUserId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoAddresses() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByUserId(1L))
                .thenReturn(List.of());

        List<AddressResponse> response = addressService.getUserAddresses(
                "test@example.com");

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(addressRepository)
                .findByUserId(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringGet() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.getUserAddresses(
                        "test@example.com"));

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository, never())
                .findByUserId(anyLong());
    }

    // ---------------------------------------------------------
    // UPDATE ADDRESS
    // ---------------------------------------------------------

    @Test
    void shouldUpdateAddressSuccessfully() {

        request.setFullName("Updated Name");
        request.setPhoneNumber("9123456789");
        request.setAddressLine1("Updated Street");
        request.setAddressLine2("Updated Area");
        request.setCity("Bhubaneswar");
        request.setState("Odisha");
        request.setPostalCode("751001");
        request.setCountry("India");
        request.setIsDefault(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(address));

        when(addressRepository.save(any(UserAddress.class)))
                .thenReturn(address);

        AddressResponse response = addressService.updateAddress(
                "test@example.com",
                1L,
                request);

        assertNotNull(response);

        assertEquals(
                "Updated Name",
                response.getFullName());

        assertEquals(
                "9123456789",
                response.getPhoneNumber());

        assertEquals(
                "Updated Street",
                response.getAddressLine1());

        assertEquals(
                "Updated Area",
                response.getAddressLine2());

        assertEquals(
                "Bhubaneswar",
                response.getCity());

        assertEquals(
                "751001",
                response.getPostalCode());

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository)
                .findByIdAndUserId(1L, 1L);

        verify(addressRepository)
                .save(any(UserAddress.class));
    }

    @Test
    void shouldUpdateAddressAndMakeItDefault() {

        request.setIsDefault(true);

        UserAddress oldDefaultAddress = UserAddress.builder()
                .id(2L)
                .user(user)
                .fullName("Old Address")
                .phoneNumber("9999999999")
                .addressLine1("Old Street")
                .city("Brahmapur")
                .state("Odisha")
                .postalCode("760002")
                .country("India")
                .isDefault(true)
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(address));

        when(addressRepository.findByUserId(1L))
                .thenReturn(List.of(address, oldDefaultAddress));

        when(addressRepository.save(any(UserAddress.class)))
                .thenReturn(address);

        AddressResponse response = addressService.updateAddress(
                "test@example.com",
                1L,
                request);

        assertNotNull(response);

        assertTrue(response.getIsDefault());

        assertFalse(oldDefaultAddress.getIsDefault());

        verify(addressRepository)
                .findByUserId(1L);

        verify(addressRepository)
                .save(any(UserAddress.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringUpdate() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.updateAddress(
                        "test@example.com",
                        1L,
                        request));

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldThrowExceptionWhenAddressNotFoundDuringUpdate() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.updateAddress(
                        "test@example.com",
                        1L,
                        request));

        verify(addressRepository)
                .findByIdAndUserId(1L, 1L);

        verify(addressRepository, never())
                .save(any(UserAddress.class));
    }

    // ---------------------------------------------------------
    // DELETE ADDRESS
    // ---------------------------------------------------------

    @Test
    void shouldDeleteAddressSuccessfully() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(address));

        addressService.deleteAddress(
                "test@example.com",
                1L);

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository)
                .findByIdAndUserId(1L, 1L);

        verify(addressRepository)
                .delete(address);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringDelete() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.deleteAddress(
                        "test@example.com",
                        1L));

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(addressRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(addressRepository, never())
                .delete(any(UserAddress.class));
    }

    @Test
    void shouldThrowExceptionWhenAddressNotFoundDuringDelete() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(addressRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> addressService.deleteAddress(
                        "test@example.com",
                        1L));

        verify(addressRepository)
                .findByIdAndUserId(1L, 1L);

        verify(addressRepository, never())
                .delete(any(UserAddress.class));
    }
}