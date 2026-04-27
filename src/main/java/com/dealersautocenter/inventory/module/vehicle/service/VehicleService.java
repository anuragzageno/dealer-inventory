package com.dealersautocenter.inventory.module.vehicle.service;

import com.dealersautocenter.inventory.module.dealer.domain.Dealer;
import com.dealersautocenter.inventory.module.dealer.repository.DealerRepository;
import com.dealersautocenter.inventory.module.vehicle.domain.Vehicle;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleFilterParams;
import com.dealersautocenter.inventory.module.vehicle.dto.VehiclePatchRequest;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleRequest;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleResponse;
import com.dealersautocenter.inventory.module.vehicle.repository.VehicleRepository;
import com.dealersautocenter.inventory.module.vehicle.repository.VehicleSpecification;
import com.dealersautocenter.inventory.shared.exception.ResourceNotFoundException;
import com.dealersautocenter.inventory.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DealerRepository dealerRepository;

    public VehicleResponse create(VehicleRequest request) {
        String tenantId = TenantContext.get();

        // The dealer must belong to the same tenant as the caller
        Dealer dealer = dealerRepository.findByIdAndTenantId(request.dealerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dealer not found: " + request.dealerId()));

        Vehicle vehicle = Vehicle.builder()
                .tenantId(tenantId)
                .dealer(dealer)
                .model(request.model())
                .price(request.price())
                .status(request.status())
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(UUID id) {
        return vehicleRepository.findByIdAndTenantId(id, TenantContext.get())
                .map(VehicleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> findAll(VehicleFilterParams params, Pageable pageable) {
        String tenantId = TenantContext.get();
        return vehicleRepository
                .findAll(VehicleSpecification.withFilters(tenantId, params), pageable)
                .map(VehicleResponse::from);
    }

    public VehicleResponse patch(UUID id, VehiclePatchRequest request) {
        String tenantId = TenantContext.get();

        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));

        if (request.dealerId() != null) {
            Dealer dealer = dealerRepository.findByIdAndTenantId(request.dealerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Dealer not found: " + request.dealerId()));
            vehicle.setDealer(dealer);
        }

        if (request.model() != null) {
            vehicle.setModel(request.model());
        }
        if (request.price() != null) {
            vehicle.setPrice(request.price());
        }
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        vehicleRepository.delete(vehicle);
    }
}
