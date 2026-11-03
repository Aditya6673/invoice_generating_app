package com.rishav.billing.catalog.service;

import com.rishav.billing.catalog.dto.CreateProductRequest;
import com.rishav.billing.catalog.dto.ProductDto;
import com.rishav.billing.catalog.dto.UpdateProductRequest;
import com.rishav.billing.catalog.entity.Product;
import com.rishav.billing.catalog.repository.ProductRepository;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.BillingUserDetails;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);

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
    void testCreateProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Accounting Software License",
                "998311",
                new BigDecimal("5000.00"),
                new BigDecimal("18.00"),
                "NOS"
        );

        Product saved = new Product();
        saved.setId(UUID.randomUUID());
        saved.setBusinessId(businessId);
        saved.setName(request.name());
        saved.setHsnSacCode(request.hsnSacCode());
        saved.setDefaultPrice(request.defaultPrice());
        saved.setDefaultTaxRate(request.defaultTaxRate());
        saved.setUnit(request.unit());
        saved.setActive(true);
        saved.setCreatedAt(Instant.now());

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDto result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals("Accounting Software License", result.name());
        assertEquals("998311", result.hsnSacCode());
        assertEquals(new BigDecimal("5000.00"), result.defaultPrice());
        assertEquals(new BigDecimal("18.00"), result.defaultTaxRate());
        assertEquals("NOS", result.unit());
        assertTrue(result.active());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product captured = captor.getValue();
        assertEquals(businessId, captured.getBusinessId());
        assertEquals(userId, captured.getCreatedBy());
        assertEquals(userId, captured.getUpdatedBy());
        assertTrue(captured.isActive());
    }

    @Test
    void testGetProduct_Success() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setBusinessId(businessId);
        product.setName("Widget A");

        when(productRepository.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.of(product));

        ProductDto dto = productService.getProduct(productId);

        assertNotNull(dto);
        assertEquals(productId, dto.id());
        assertEquals("Widget A", dto.name());
    }

    @Test
    void testGetProduct_NotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(productId));
    }

    @Test
    void testUpdateProduct_Success() {
        UUID productId = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(productId);
        existing.setBusinessId(businessId);
        existing.setName("Old Widget");
        existing.setDefaultPrice(new BigDecimal("100.00"));
        existing.setDefaultTaxRate(new BigDecimal("12.00"));
        existing.setUnit("PCS");
        existing.setActive(true);

        when(productRepository.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(
                "New Widget",
                null,
                new BigDecimal("150.00"),
                null,
                null,
                null
        );

        ProductDto updated = productService.updateProduct(productId, request);

        assertEquals("New Widget", updated.name());
        assertEquals(new BigDecimal("150.00"), existing.getDefaultPrice());
        assertEquals(new BigDecimal("12.00"), existing.getDefaultTaxRate()); // preserved
    }

    @Test
    void testListProducts_WithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Widget Pro");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByBusinessIdAndActiveTrueAndNameContainingIgnoreCase(businessId, "Pro", pageable))
                .thenReturn(page);

        PageResponse<ProductDto> response = productService.listProducts("Pro", pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals("Widget Pro", response.content().getFirst().name());
    }

    @Test
    void testListProducts_WithoutSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Widget Pro");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByBusinessIdAndActiveTrue(businessId, pageable)).thenReturn(page);

        PageResponse<ProductDto> response = productService.listProducts(null, pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
    }

    @Test
    void testDeactivateProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setBusinessId(businessId);
        product.setActive(true);

        when(productRepository.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.of(product));

        productService.deactivateProduct(productId);

        assertFalse(product.isActive());
        assertEquals(userId, product.getUpdatedBy());
        verify(productRepository).save(product);
    }

    @Test
    void testGetProductEntity() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setBusinessId(businessId);

        when(productRepository.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.of(product));

        Product entity = productService.getProductEntity(productId);

        assertNotNull(entity);
        assertEquals(productId, entity.getId());
    }
}
