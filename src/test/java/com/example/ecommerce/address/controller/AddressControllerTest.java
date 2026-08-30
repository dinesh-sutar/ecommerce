package com.example.ecommerce.address.controller;

import com.example.ecommerce.address.dto.AddressResponse;
import com.example.ecommerce.address.dto.CreateAddressRequest;
import com.example.ecommerce.address.service.AddressService;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.security.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressService addressService;

    /*
     * Required because JwtAuthenticationFilter depends on JwtService.
     * WebMvcTest does not load the real JwtService bean.
     */
    @MockBean
    private JwtService jwtService;

    /*
     * With addFilters = false, the security filter chain never runs, so
     * SecurityMockMvcRequestPostProcessors.user(...) has no effect on the
     * request's Principal (it only populates the SecurityContext, which
     * normally gets copied onto the request by a filter). Controller
     * methods take Authentication/Principal as a method argument, which
     * Spring MVC resolves from HttpServletRequest.getUserPrincipal() —
     * so we set it directly on each request instead.
     */
    private final Principal principal = new UsernamePasswordAuthenticationToken(
            "test@example.com", null, List.of());

    private AddressResponse addressResponse;

    private CreateAddressRequest request;

    @BeforeEach
    void setUp() {

        addressResponse = AddressResponse.builder()
                .id(1L)
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

    // =========================================================
    // CREATE ADDRESS
    // =========================================================

    @Test
    void createAddress_withValidRequest_returns201()
            throws Exception {

        when(addressService.createAddress(
                eq("test@example.com"),
                any(CreateAddressRequest.class)))
                .thenReturn(addressResponse);

        mockMvc.perform(
                post("/api/addresses")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName")
                        .value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber")
                        .value("9876543210"))
                .andExpect(jsonPath("$.addressLine1")
                        .value("123 Main Street"))
                .andExpect(jsonPath("$.addressLine2")
                        .value("Near City Mall"))
                .andExpect(jsonPath("$.city")
                        .value("Brahmapur"))
                .andExpect(jsonPath("$.state")
                        .value("Odisha"))
                .andExpect(jsonPath("$.postalCode")
                        .value("760001"))
                .andExpect(jsonPath("$.country")
                        .value("India"))
                .andExpect(jsonPath("$.isDefault")
                        .value(false));

        verify(addressService)
                .createAddress(
                        eq("test@example.com"),
                        any(CreateAddressRequest.class));
    }

    @Test
    void createAddress_whenUserNotFound_returns404()
            throws Exception {

        when(addressService.createAddress(
                eq("test@example.com"),
                any(CreateAddressRequest.class)))
                .thenThrow(
                        new ResourceNotFoundException(
                                "User not found"));

        mockMvc.perform(
                post("/api/addresses")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("User not found"));
    }

    // =========================================================
    // GET ADDRESSES
    // =========================================================

    @Test
    void getUserAddresses_returnsAddresses()
            throws Exception {

        when(addressService.getUserAddresses(
                eq("test@example.com")))
                .thenReturn(List.of(addressResponse));

        mockMvc.perform(
                get("/api/addresses")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(1))
                .andExpect(jsonPath("$[0].fullName")
                        .value("John Doe"))
                .andExpect(jsonPath("$[0].phoneNumber")
                        .value("9876543210"))
                .andExpect(jsonPath("$[0].addressLine1")
                        .value("123 Main Street"))
                .andExpect(jsonPath("$[0].city")
                        .value("Brahmapur"))
                .andExpect(jsonPath("$[0].state")
                        .value("Odisha"))
                .andExpect(jsonPath("$[0].postalCode")
                        .value("760001"))
                .andExpect(jsonPath("$[0].country")
                        .value("India"))
                .andExpect(jsonPath("$[0].isDefault")
                        .value(false));

        verify(addressService)
                .getUserAddresses("test@example.com");
    }

    @Test
    void getUserAddresses_whenNoAddresses_returnsEmptyList()
            throws Exception {

        when(addressService.getUserAddresses(
                eq("test@example.com")))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/addresses")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(addressService)
                .getUserAddresses("test@example.com");
    }

    // =========================================================
    // UPDATE ADDRESS
    // =========================================================

    @Test
    void updateAddress_withValidRequest_returns200()
            throws Exception {

        request.setFullName("Updated John Doe");
        request.setCity("Bhubaneswar");
        request.setPostalCode("751001");

        AddressResponse updatedResponse = AddressResponse.builder()
                .id(1L)
                .fullName("Updated John Doe")
                .phoneNumber("9876543210")
                .addressLine1("123 Main Street")
                .addressLine2("Near City Mall")
                .city("Bhubaneswar")
                .state("Odisha")
                .postalCode("751001")
                .country("India")
                .isDefault(false)
                .build();

        when(addressService.updateAddress(
                eq("test@example.com"),
                eq(1L),
                any(CreateAddressRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(
                put("/api/addresses/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.fullName")
                        .value("Updated John Doe"))
                .andExpect(jsonPath("$.phoneNumber")
                        .value("9876543210"))
                .andExpect(jsonPath("$.addressLine1")
                        .value("123 Main Street"))
                .andExpect(jsonPath("$.city")
                        .value("Bhubaneswar"))
                .andExpect(jsonPath("$.postalCode")
                        .value("751001"));

        verify(addressService)
                .updateAddress(
                        eq("test@example.com"),
                        eq(1L),
                        any(CreateAddressRequest.class));
    }

    @Test
    void updateAddress_whenAddressNotFound_returns404()
            throws Exception {

        when(addressService.updateAddress(
                eq("test@example.com"),
                eq(999L),
                any(CreateAddressRequest.class)))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Address not found"));

        mockMvc.perform(
                put("/api/addresses/999")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Address not found"));
    }

    // =========================================================
    // DELETE ADDRESS
    // =========================================================

    @Test
    void deleteAddress_returns204()
            throws Exception {

        doNothing()
                .when(addressService)
                .deleteAddress(
                        eq("test@example.com"),
                        eq(1L));

        mockMvc.perform(
                delete("/api/addresses/1")
                        .principal(principal))
                .andExpect(status().isNoContent());

        verify(addressService)
                .deleteAddress(
                        "test@example.com",
                        1L);
    }

    @Test
    void deleteAddress_whenAddressNotFound_returns404()
            throws Exception {

        doThrow(
                new ResourceNotFoundException(
                        "Address not found"))
                .when(addressService)
                .deleteAddress(
                        eq("test@example.com"),
                        eq(999L));

        mockMvc.perform(
                delete("/api/addresses/999")
                        .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Address not found"));

        verify(addressService)
                .deleteAddress(
                        "test@example.com",
                        999L);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void createAddress_withInvalidRequest_returns400()
            throws Exception {

        CreateAddressRequest invalidRequest = new CreateAddressRequest();

        mockMvc.perform(
                post("/api/addresses")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAddress_withInvalidRequest_returns400()
            throws Exception {

        CreateAddressRequest invalidRequest = new CreateAddressRequest();

        mockMvc.perform(
                put("/api/addresses/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}