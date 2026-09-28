package com.example.ecommerce_api.product.sevices;

import com.example.ecommerce_api.product.dto.ImageRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {


    String uploadImage(ImageRequest imageRequest);
}
