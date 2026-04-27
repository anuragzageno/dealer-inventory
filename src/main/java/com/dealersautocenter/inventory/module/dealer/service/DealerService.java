package com.dealersautocenter.inventory.module.dealer.service;

import com.dealersautocenter.inventory.module.dealer.domain.Dealer;
import com.dealersautocenter.inventory.module.dealer.dto.DealerPatchRequest;
import com.dealersautocenter.inventory.module.dealer.dto.DealerRequest;
import com.dealersautocenter.inventory.module.dealer.dto.DealerResponse;
import com.dealersautocenter.inventory.module.dealer.repository.DealerRepository;
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
public class DealerService {

    private final DealerRepository dealerRepository;

    public DealerResponse create(DealerRequest request) {
        String tenantId = TenantContext.get();

        if (dealerRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new IllegalArgumentException(
                    "A dealer with email '" + request.email() + "' already exists in this tenant");
        }

        Dealer dealer = Dealer.builder()
                .tenantId(tenantId)
                .name(request.name())
                .email(request.email())
                .subscriptionType(request.subscriptionType())
                .build();

        return DealerResponse.from(dealerRepository.save(dealer));
    }

    @Transactional(readOnly = true)
    public DealerResponse findById(UUID id) {
        return dealerRepository.findByIdAndTenantId(id, TenantContext.get())
                .map(DealerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<DealerResponse> findAll(Pageable pageable) {
        return dealerRepository.findAllByTenantId(TenantContext.get(), pageable)
                .map(DealerResponse::from);
    }

    public DealerResponse patch(UUID id, DealerPatchRequest request) {
        String tenantId = TenantContext.get();

        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + id));

        if (request.name() != null) {
            dealer.setName(request.name());
        }

        if (request.email() != null && !request.email().equals(dealer.getEmail())) {
            if (dealerRepository.existsByEmailAndTenantIdAndIdNot(request.email(), tenantId, id)) {
                throw new IllegalArgumentException(
                        "Email '" + request.email() + "' is already in use within this tenant");
            }
            dealer.setEmail(request.email());
        }

        if (request.subscriptionType() != null) {
            dealer.setSubscriptionType(request.subscriptionType());
        }

        return DealerResponse.from(dealerRepository.save(dealer));
    }

    public void delete(UUID id) {
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + id));
        dealerRepository.delete(dealer);
    }
}
