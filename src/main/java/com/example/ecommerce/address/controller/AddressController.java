package com.example.ecommerce.address.controller;

import com.example.ecommerce.address.dto.AddressResponse;
import com.example.ecommerce.address.dto.CreateAddressRequest;
import com.example.ecommerce.address.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(
            Authentication authentication,
            @Valid @RequestBody CreateAddressRequest request) {

        return addressService.createAddress(
                authentication.getName(),
                request);
    }

    @GetMapping
    public List<AddressResponse> getUserAddresses(
            Authentication authentication) {

        return addressService.getUserAddresses(
                authentication.getName());
    }

    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateAddressRequest request) {

        return addressService.updateAddress(
                authentication.getName(),
                addressId,
                request);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        addressService.deleteAddress(
                authentication.getName(),
                addressId);
    }
}