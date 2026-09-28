package com.example.ecommerce_api.user.mapper;


import com.example.ecommerce_api.user.dto.request.AddressRequest;
import com.example.ecommerce_api.user.dto.response.AddressResponse;
import com.example.ecommerce_api.user.entity.Address;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressMapper {

    public Address toEntity(AddressRequest request) {
        Address address = new Address();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        if (request.getIsDefault() != null) {
            address.setDefaultAddress(request.getIsDefault());
        }
        return address;
    }

    public Address updateEntity(Address address, AddressRequest request) {
        if (request == null) {
            return address;
        }

        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            address.setZipCode(request.getZipCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getIsDefault() != null) {
            address.setDefaultAddress(request.getIsDefault());
        }
        return address;
    }

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.isDefaultAddress())
                .build();
    }

    public List<AddressResponse> toResponseList(List<Address> addresses) {
        if (addresses == null) {
            return List.of();
        }
        return addresses.stream().map(this::toResponse).toList();
    }
}
