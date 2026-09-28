package com.example.ecommerce_api.user.services.impl;

import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.user.dto.request.AddressRequest;
import com.example.ecommerce_api.user.dto.response.AddressResponse;
import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.mapper.AddressMapper;
import com.example.ecommerce_api.user.repository.AddressRepository;
import com.example.ecommerce_api.user.repository.UserRepository;
import com.example.ecommerce_api.user.services.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressMapper.toResponseList(addressRepository.findAllByUserId(userId));
    }

    @Override
    public AddressResponse getAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressMapper.toEntity(request);

        boolean isFirstAddress = !addressRepository.existsByUserId(userId);
        if (isFirstAddress) {
            address.setDefaultAddress(true);
        }

        user.addAddress(address);
        userRepository.save(user);

        if (address.isDefaultAddress()) {
            addressRepository.clearDefaultForOtherAddresses(userId, address.getId());
        }

        log.info("Added address {} for user {}", address.getId(), userId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {

        Address address = findOwnedAddress(userId, addressId);
        addressMapper.updateEntity(address, request);
        addressRepository.save(address);

        if (address.isDefaultAddress()) {
            addressRepository.clearDefaultForOtherAddresses(userId, addressId);
        }

        log.info("Updated address {} for user {}", addressId, userId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        addressRepository.delete(address);
        log.info("Deleted address {} for user {}", addressId, userId);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {

        Address address = findOwnedAddress(userId, addressId);
        address.setDefaultAddress(true);
        addressRepository.save(address);
        addressRepository.clearDefaultForOtherAddresses(userId, addressId);

        log.info("Set address {} as default for user {}", addressId, userId);
        return addressMapper.toResponse(address);
    }

    private Address findOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
