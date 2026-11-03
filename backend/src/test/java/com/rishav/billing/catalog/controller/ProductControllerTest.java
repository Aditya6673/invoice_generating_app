package com.rishav.billing.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishav.billing.catalog.dto.CreateProductRequest;
import com.rishav.billing.catalog.dto.ProductDto;
import com.rishav.billing.catalog.dto.UpdateProductRequest;
import com.rishav.billing.catalog.service.ProductService;
import com.rishav.billing.common.dto.PageResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testCreateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                "Widget A", "84713010", new BigDecimal("1200.00"), new BigDecimal("18.00"), "NOS");

        ProductDto dto = new ProductDto(
                id, "Widget A", "84713010", new BigDecimal("1200.00"), new BigDecimal("18.00"), "NOS", true, Instant.now());

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Widget A"));
    }

    @Test
    void testGetProduct() throws Exception {
        UUID id = UUID.randomUUID();
        ProductDto dto = new ProductDto(
                id, "Widget A", "84713010", new BigDecimal("1200.00"), new BigDecimal("18.00"), "NOS", true, Instant.now());

        when(productService.getProduct(id)).thenReturn(dto);

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void testListProducts() throws Exception {
        UUID id = UUID.randomUUID();
        ProductDto dto = new ProductDto(
                id, "Widget A", "84713010", new BigDecimal("1200.00"), new BigDecimal("18.00"), "NOS", true, Instant.now());
        PageResponse<ProductDto> pageResponse = new PageResponse<>(List.of(dto), 0, 20, 1, 1, true);

        when(productService.listProducts(eq("Widget"), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/products").param("search", "Widget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Widget A"));
    }

    @Test
    void testUpdateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Widget", null, new BigDecimal("1500.00"), null, null, null);

        ProductDto dto = new ProductDto(
                id, "Updated Widget", "84713010", new BigDecimal("1500.00"), new BigDecimal("18.00"), "NOS", true, Instant.now());

        when(productService.updateProduct(eq(id), any(UpdateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Widget"));
    }

    @Test
    void testDeactivateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).deactivateProduct(id);

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).deactivateProduct(id);
    }
}
