package com.example.ecommerce_api.Enum;

public enum OrderStatus {

    PENDING("Order received, waiting for confirmation"),
    PROCESSING("Order is being packed"),
    SHIPPED("Order is on the way"),
    DELIVERED("Order delivered successfully"),
    CANCELLED("Order was cancelled");

    private String description;


    OrderStatus(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public  boolean isFinalStatus()
    {
        return this == DELIVERED || this == CANCELLED;
    }
}
