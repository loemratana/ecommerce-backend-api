package com.example.ecommerce_api.product.sevices;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

     String uploadFile(MultipartFile file,String folder);
}
