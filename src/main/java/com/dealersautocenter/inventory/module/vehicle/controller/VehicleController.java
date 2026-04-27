package com.dealersautocenter.inventory.module.vehicle.controller;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleFilterParams;
import com.dealersautocenter.inventory.module.vehicle.dto.VehiclePatchRequest;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleRequest;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleResponse;
import com.dealersautocenter.inventory.module.vehicle.domain.VehicleStatus;
import com.dealersautocenter.inventory.module.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request);
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable UUID id) {
        return vehicleService.findById(id);
    }

    /**
     * Lists vehicles for the caller's tenant with optional filters and pagination.
     *
     * <p>When {@code subscription=PREMIUM} is provided only vehicles belonging to PREMIUM
     * dealers within the caller's tenant are returned (tenant scope is never lifted).
     *
     * <p>Supports standard Spring {@code page}, {@code size}, and {@code sort} query params.
     */
    @GetMapping
    public Page<VehicleResponse> getAll(
            @RequestParam(required = false) String model,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) SubscriptionType subscription,
            @PageableDefault(size = 20, sort = "model") Pageable pageable) {

        VehicleFilterParams params =
                new VehicleFilterParams(model, status, priceMin, priceMax, subscription);
        return vehicleService.findAll(params, pageable);
    }

    @PatchMapping("/{id}")
    public VehicleResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody VehiclePatchRequest request) {
        return vehicleService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        vehicleService.delete(id);
    }
}
