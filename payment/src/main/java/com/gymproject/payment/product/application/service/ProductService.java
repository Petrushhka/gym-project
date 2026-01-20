package com.gymproject.payment.product.application.service;

import com.gymproject.common.dto.payment.ProductInfo;
import com.gymproject.payment.application.dto.ProductResponse;
import com.gymproject.payment.product.application.dto.CreateProductRequest;
import com.gymproject.payment.product.domain.entity.Product;
import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import com.gymproject.payment.product.exception.ProductErrorCode;
import com.gymproject.payment.product.exception.ProductException;
import com.gymproject.payment.product.infrastructure.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 1. 상품 생성
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        if(productRepository.existsByCodeAndStatus(request.code(), ProductStatus.ACTIVE)){
            throw new ProductException(ProductErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        Product product = Product.createProduct(
                request.name(),
                request.code(),
                request.price(),
                request.category()
        );

        productRepository.save(product);

        return ProductResponse.create(product);
    }

    // 2. 상품 삭제(Soft Delete)
    @Transactional
    public ProductResponse deleteProduct(Long productId) {

        Product product = getProduct(productId);

        product.delete();

        productRepository.save(product);

        return ProductResponse.create(product);
    }

    // 3. 전 상품 목록 조회
    public List<Product> searchProducts(ProductCategory category, ProductStatus status) {
        return productRepository.searchProducts(category, status);
    }

    // -- 어댑터용 상품 정보 만들어주기(영수증 및 구매용 상품 정보 전달)
    public ProductInfo getProductInfo(Long productId) {
        Product product = getProduct(productId);

        return new ProductInfo(
                product.getName(),
                product.getPrice(),
                product.getCode()
        );
    }

    public Product getProduct(Long productId){
        return productRepository.findById(productId)
                .orElseThrow(()->  new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
