package com.example.ecommerce_api.product.controller;

import com.example.ecommerce_api.product.dto.ImageRequest;
import com.example.ecommerce_api.product.sevices.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Upload an image", description = "Uploads the file to Cloudinary and returns its URL; use that URL as imageUrl / imageCover")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Image uploaded"),
            @ApiResponse(responseCode = "400", description = "Missing file or name")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@ModelAttribute ImageRequest request) {
        String url = imageService.uploadImage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }
}
