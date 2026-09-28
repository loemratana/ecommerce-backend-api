package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.user.dto.response.DeviceResponse;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.entity.UserDevice;

import java.util.List;

public interface UserDeviceService {

    UserDevice findOrCreateDevice(User user, String deviceId, String deviceName, String deviceType);

    List<DeviceResponse> listDevices(Long userId, Long currentDeviceId);

    void revokeDevice(Long userId, Long deviceId);
}
