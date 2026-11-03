package com.rishav.billing.customer.service;

import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import com.rishav.billing.customer.dto.CreateCustomerRequest;
import com.rishav.billing.customer.dto.CustomerDto;
import com.rishav.billing.customer.dto.UpdateCustomerRequest;
import com.rishav.billing.customer.entity.Customer;
import com.rishav.billing.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerDto createCustomer(CreateCustomerRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Customer customer = new Customer();
        customer.setBusinessId(businessId);
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setBillingAddress(request.billingAddress());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setStateCode(request.stateCode());
        customer.setPincode(request.pincode());
        customer.setGstin(request.gstin());
        customer.setDeleted(false);

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            customer.setCreatedBy(userId);
            customer.setUpdatedBy(userId);
        }

        Customer saved = customerRepository.save(customer);
        return CustomerDto.fromEntity(saved);
    }

    public CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        if (request.email() != null) {
            customer.setEmail(request.email());
        }
        if (request.billingAddress() != null) {
            customer.setBillingAddress(request.billingAddress());
        }
        if (request.city() != null) {
            customer.setCity(request.city());
        }
        if (request.state() != null) {
            customer.setState(request.state());
        }
        if (request.stateCode() != null) {
            customer.setStateCode(request.stateCode());
        }
        if (request.pincode() != null) {
            customer.setPincode(request.pincode());
        }
        if (request.gstin() != null) {
            customer.setGstin(request.gstin());
        }

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            customer.setUpdatedBy(userId);
        }

        Customer updated = customerRepository.save(customer);
        return CustomerDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return CustomerDto.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> listCustomers(String search, Pageable pageable) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Page<Customer> page;
        if (search != null && !search.trim().isEmpty()) {
            page = customerRepository.findByBusinessIdAndNameContainingIgnoreCase(businessId, search.trim(), pageable);
        } else {
            page = customerRepository.findByBusinessId(businessId, pageable);
        }
        return PageResponse.from(page.map(CustomerDto::fromEntity));
    }

    public void deleteCustomer(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setDeleted(true);

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            customer.setUpdatedBy(userId);
        }

        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntity(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        return customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }
}
