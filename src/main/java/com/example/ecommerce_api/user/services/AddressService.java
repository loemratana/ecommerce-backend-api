package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.user.dto.request.AddressRequest;
import com.example.ecommerce_api.user.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse getAddress(Long userId, Long addressId);

    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
