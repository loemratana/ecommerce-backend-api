package com.example.ecommerce_api.common.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter
@Setter
public class  ApiResponse <T> {


    private String message;
    private boolean success;
    private T data;
    private Instant timestamp = Instant.now();


    public ApiResponse(String message,boolean success,T data){
        this.message = message;
        this.success = success;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>("OK",true,data);
    }

    public static <T> ApiResponse<T> success(T data,String message){

        return new ApiResponse<>(message,true,data);
    }

}
