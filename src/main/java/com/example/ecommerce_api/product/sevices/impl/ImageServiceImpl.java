package com.example.ecommerce_api.product.sevices.impl;

import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.product.dto.ImageRequest;
import com.example.ecommerce_api.product.sevices.CloudinaryService;
import com.example.ecommerce_api.product.sevices.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final CloudinaryService cloudinaryService;

    @Override
    public String uploadImage(ImageRequest imageRequest) {

        if (imageRequest.getFile() == null || imageRequest.getFile().isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (imageRequest.getName() == null || imageRequest.getName().isEmpty()) {
            throw new BadRequestException("Image name is required");
        }

        return cloudinaryService.uploadFile(imageRequest.getFile(), imageRequest.getName());
    }

}
