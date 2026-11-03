package com.rishav.billing.catalog.service;

import com.rishav.billing.catalog.dto.CreateProductRequest;
import com.rishav.billing.catalog.dto.ProductDto;
import com.rishav.billing.catalog.dto.UpdateProductRequest;
import com.rishav.billing.catalog.entity.Product;
import com.rishav.billing.catalog.repository.ProductRepository;
import com.rishav.billing.common.dto.PageResponse;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductDto createProduct(CreateProductRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        Product product = new Product();
        product.setBusinessId(businessId);
        product.setName(request.name());
        product.setHsnSacCode(request.hsnSacCode());
        product.setDefaultPrice(request.defaultPrice());
        product.setDefaultTaxRate(request.defaultTaxRate());
        product.setUnit(request.unit() != null ? request.unit() : "PCS");
        product.setActive(true);

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            product.setCreatedBy(userId);
            product.setUpdatedBy(userId);
        }

        Product saved = productRepository.save(product);
        return ProductDto.fromEntity(saved);
    }

    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Product product = productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.hsnSacCode() != null) {
            product.setHsnSacCode(request.hsnSacCode());
        }
        if (request.defaultPrice() != null) {
            product.setDefaultPrice(request.defaultPrice());
        }
        if (request.defaultTaxRate() != null) {
            product.setDefaultTaxRate(request.defaultTaxRate());
        }
        if (request.unit() != null) {
            product.setUnit(request.unit());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            product.setUpdatedBy(userId);
        }

        Product updated = productRepository.save(product);
        return ProductDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Product product = productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductDto.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listProducts(String search, Pageable pageable) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Page<Product> page;
        if (search != null && !search.trim().isEmpty()) {
            page = productRepository.findByBusinessIdAndActiveTrueAndNameContainingIgnoreCase(businessId, search.trim(), pageable);
        } else {
            page = productRepository.findByBusinessIdAndActiveTrue(businessId, pageable);
        }
        return PageResponse.from(page.map(ProductDto::fromEntity));
    }

    public void deactivateProduct(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Product product = productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(false);

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            product.setUpdatedBy(userId);
        }

        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(UUID id) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        return productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
}
