package com.dealersautocenter.inventory.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CrossTenantAccessException extends RuntimeException {

    public CrossTenantAccessException() {
        super("Cross-tenant access denied");
    }

    public CrossTenantAccessException(String message) {
        super(message);
    }
}
