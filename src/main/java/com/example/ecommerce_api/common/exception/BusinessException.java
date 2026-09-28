package com.example.ecommerce_api.common.exception;

import java.util.List;
import java.util.Set;

public class BusinessException extends RuntimeException {

    private List<Long> notfoundRoleIds;
    public BusinessException(String message, List<Long> notfoundRoleIds) {
        super(String.format( "%s. Role IDs not found: %s",
                message,
                notfoundRoleIds));
        this.notfoundRoleIds = List.copyOf(notfoundRoleIds);
    }
    public BusinessException(String message) {
        super(message);
    }
}
