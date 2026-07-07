package com.pricing.engine.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pricing.engine.dto.request.ProductRequest;
import com.pricing.engine.dto.response.ProductResponse;
import com.pricing.engine.entity.Product;
import com.pricing.engine.exception.DuplicateResourceException;
import com.pricing.engine.exception.ResourceNotFoundException;
import com.pricing.engine.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            log.warn("Rejected duplicate product name: {}", request.getName());
            throw new DuplicateResourceException(
                    "A product named '" + request.getName() + "' already exists");
        }
        Product product = Product.builder()
                .name(request.getName())
                .basePrice(request.getBasePrice())
                .inventoryCount(request.getInventoryCount())
                .build();
        Product saved = productRepository.save(product);
        log.info("Created product id={} name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    // Updating a product (everything while update) (e.g. inventory count) can change the outcome of an
    // inventory-based rule, so the cached price must be evicted.
    @Transactional
    @CacheEvict(value = "priceCache", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        if (productRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            log.warn("Rejected update: product name '{}' already used by another product", request.getName());
            throw new DuplicateResourceException(
                    "A product named '" + request.getName() + "' already exists");
        }
        product.setName(request.getName());
        product.setBasePrice(request.getBasePrice());
        product.setInventoryCount(request.getInventoryCount());
        Product saved = productRepository.save(product);
        log.info("Updated product id={}, cache evicted", id);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "priceCache", key = "#id")
    public void deleteProduct(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
        log.info("Deleted product id={}", id);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .inventoryCount(product.getInventoryCount())
                .build();
    }
}
