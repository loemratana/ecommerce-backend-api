package com.example.ecommerce_api.product.sevices.impl;

import com.cloudinary.Cloudinary;
import com.example.ecommerce_api.product.sevices.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folder) {

        try
        {

            HashMap<String,Object> options  = new HashMap<>();
            options.put("folder", folder);
            Map uploadedFile = cloudinary.uploader().upload(file.getBytes(),options);
            String publicId  = (String) uploadedFile.get("public_id");
            return cloudinary.url().secure(true).generate(publicId);


        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
