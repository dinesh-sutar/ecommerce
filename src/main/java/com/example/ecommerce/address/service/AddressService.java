package com.example.ecommerce.address.service;

import com.example.ecommerce.address.dto.AddressResponse;
import com.example.ecommerce.address.dto.CreateAddressRequest;
import com.example.ecommerce.address.entity.UserAddress;
import com.example.ecommerce.address.repository.AddressRepository;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse createAddress(
            String email,
            CreateAddressRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {

            addressRepository.findByUserId(user.getId())
                    .forEach(address -> address.setIsDefault(false));
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(
                        Boolean.TRUE.equals(
                                request.getIsDefault()))
                .build();

        return mapToResponse(
                addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return addressRepository
                .findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AddressResponse updateAddress(
            String email,
            Long addressId,
            CreateAddressRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {

            addressRepository.findByUserId(user.getId())
                    .forEach(existingAddress -> existingAddress.setIsDefault(false));
        }

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        return mapToResponse(
                addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(
            String email,
            Long addressId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found"));

        addressRepository.delete(address);
    }

    private AddressResponse mapToResponse(UserAddress address) {

        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}