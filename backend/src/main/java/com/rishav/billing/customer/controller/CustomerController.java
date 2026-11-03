package com.rishav.billing.customer.controller;

import com.rishav.billing.common.dto.ApiResponse;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.customer.dto.CreateCustomerRequest;
import com.rishav.billing.customer.dto.CustomerDto;
import com.rishav.billing.customer.dto.UpdateCustomerRequest;
import com.rishav.billing.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDto customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(customer, "Customer created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> listCustomers(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CustomerDto> customers = customerService.listCustomers(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable("id") UUID id) {
        CustomerDto customer = customerService.getCustomer(id);
        return ResponseEntity.ok(ApiResponse.ok(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDto customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.ok(customer, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
