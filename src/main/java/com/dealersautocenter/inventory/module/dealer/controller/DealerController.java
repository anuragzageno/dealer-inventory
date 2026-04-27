package com.dealersautocenter.inventory.module.dealer.controller;

import com.dealersautocenter.inventory.module.dealer.dto.DealerPatchRequest;
import com.dealersautocenter.inventory.module.dealer.dto.DealerRequest;
import com.dealersautocenter.inventory.module.dealer.dto.DealerResponse;
import com.dealersautocenter.inventory.module.dealer.service.DealerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dealers")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealerResponse create(@Valid @RequestBody DealerRequest request) {
        return dealerService.create(request);
    }

    @GetMapping("/{id}")
    public DealerResponse getById(@PathVariable UUID id) {
        return dealerService.findById(id);
    }

    /**
     * Returns a paginated list of dealers for the caller's tenant.
     * Supports standard Spring {@code page}, {@code size}, and {@code sort} query parameters.
     */
    @GetMapping
    public Page<DealerResponse> getAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return dealerService.findAll(pageable);
    }

    @PatchMapping("/{id}")
    public DealerResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody DealerPatchRequest request) {
        return dealerService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        dealerService.delete(id);
    }
}
