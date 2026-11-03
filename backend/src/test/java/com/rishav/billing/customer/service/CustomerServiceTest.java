package com.rishav.billing.customer.service;

import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.BillingUserDetails;
import com.rishav.billing.customer.dto.CreateCustomerRequest;
import com.rishav.billing.customer.dto.CustomerDto;
import com.rishav.billing.customer.dto.UpdateCustomerRequest;
import com.rishav.billing.customer.entity.Customer;
import com.rishav.billing.customer.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);

        BillingUserDetails userDetails = new BillingUserDetails(
                userId, businessId, "test@example.com", "password", "ADMIN", true);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Acme Corp", "9876543210", "acme@example.com",
                "123 Street", "Mumbai", "Maharashtra", "27", "400001", "27AAAAA0000A1Z5");

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setBusinessId(businessId);
        saved.setName(request.name());
        saved.setPhone(request.phone());
        saved.setEmail(request.email());
        saved.setBillingAddress(request.billingAddress());
        saved.setCity(request.city());
        saved.setState(request.state());
        saved.setStateCode(request.stateCode());
        saved.setPincode(request.pincode());
        saved.setGstin(request.gstin());
        saved.setDeleted(false);
        saved.setCreatedAt(Instant.now());

        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerDto result = customerService.createCustomer(request);

        assertNotNull(result);
        assertEquals("Acme Corp", result.name());
        assertEquals("acme@example.com", result.email());
        assertEquals("27AAAAA0000A1Z5", result.gstin());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer captured = captor.getValue();
        assertEquals(businessId, captured.getBusinessId());
        assertEquals(userId, captured.getCreatedBy());
        assertEquals(userId, captured.getUpdatedBy());
        assertFalse(captured.isDeleted());
    }

    @Test
    void testGetCustomer_Success() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setBusinessId(businessId);
        customer.setName("Acme Corp");

        when(customerRepository.findByIdAndBusinessId(customerId, businessId)).thenReturn(Optional.of(customer));

        CustomerDto dto = customerService.getCustomer(customerId);

        assertNotNull(dto);
        assertEquals(customerId, dto.id());
        assertEquals("Acme Corp", dto.name());
    }

    @Test
    void testGetCustomer_NotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdAndBusinessId(customerId, businessId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomer(customerId));
    }

    @Test
    void testUpdateCustomer_Success() {
        UUID customerId = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(customerId);
        existing.setBusinessId(businessId);
        existing.setName("Old Name");
        existing.setPhone("1111111111");

        when(customerRepository.findByIdAndBusinessId(customerId, businessId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "New Name", null, null, null, null, null, null, null, null);

        CustomerDto updated = customerService.updateCustomer(customerId, request);

        assertEquals("New Name", updated.name());
        assertEquals("1111111111", existing.getPhone()); // preserved
    }

    @Test
    void testListCustomers_WithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Acme Corp");
        Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

        when(customerRepository.findByBusinessIdAndNameContainingIgnoreCase(businessId, "Acme", pageable))
                .thenReturn(page);

        PageResponse<CustomerDto> response = customerService.listCustomers("Acme", pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals("Acme Corp", response.content().getFirst().name());
    }

    @Test
    void testListCustomers_WithoutSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Acme Corp");
        Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

        when(customerRepository.findByBusinessId(businessId, pageable)).thenReturn(page);

        PageResponse<CustomerDto> response = customerService.listCustomers(null, pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
    }

    @Test
    void testDeleteCustomer_SoftDelete() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setBusinessId(businessId);
        customer.setDeleted(false);

        when(customerRepository.findByIdAndBusinessId(customerId, businessId)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customerId);

        assertTrue(customer.isDeleted());
        assertEquals(userId, customer.getUpdatedBy());
        verify(customerRepository).save(customer);
    }

    @Test
    void testGetCustomerEntity() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setBusinessId(businessId);

        when(customerRepository.findByIdAndBusinessId(customerId, businessId)).thenReturn(Optional.of(customer));

        Customer entity = customerService.getCustomerEntity(customerId);

        assertNotNull(entity);
        assertEquals(customerId, entity.getId());
    }
}
