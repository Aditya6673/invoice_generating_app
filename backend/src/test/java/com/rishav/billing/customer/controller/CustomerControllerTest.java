package com.rishav.billing.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.customer.dto.CreateCustomerRequest;
import com.rishav.billing.customer.dto.CustomerDto;
import com.rishav.billing.customer.dto.UpdateCustomerRequest;
import com.rishav.billing.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(customerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testCreateCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Acme Corp", "9876543210", "acme@example.com",
                "123 Street", "Mumbai", "Maharashtra", "27", "400001", "27AAAAA0000A1Z5");

        CustomerDto dto = new CustomerDto(
                id, "Acme Corp", "9876543210", "acme@example.com",
                "123 Street", "Mumbai", "Maharashtra", "27", "400001", "27AAAAA0000A1Z5", Instant.now());

        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Acme Corp"));
    }

    @Test
    void testGetCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        CustomerDto dto = new CustomerDto(
                id, "Acme Corp", "9876543210", "acme@example.com",
                "123 Street", "Mumbai", "Maharashtra", "27", "400001", "27AAAAA0000A1Z5", Instant.now());

        when(customerService.getCustomer(id)).thenReturn(dto);

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void testListCustomers() throws Exception {
        UUID id = UUID.randomUUID();
        CustomerDto dto = new CustomerDto(
                id, "Acme Corp", null, null, null, null, null, null, null, null, Instant.now());
        PageResponse<CustomerDto> pageResponse = new PageResponse<>(List.of(dto), 0, 20, 1, 1, true);

        when(customerService.listCustomers(eq("Acme"), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/customers").param("search", "Acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Acme Corp"));
    }

    @Test
    void testUpdateCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Updated Corp", null, null, null, null, null, null, null, null);

        CustomerDto dto = new CustomerDto(
                id, "Updated Corp", null, null, null, null, null, null, null, null, Instant.now());

        when(customerService.updateCustomer(eq(id), any(UpdateCustomerRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Corp"));
    }

    @Test
    void testDeleteCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(customerService).deleteCustomer(id);

        mockMvc.perform(delete("/customers/{id}", id))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(id);
    }
}
