package com.gymproject.payment.product.infrastructure.adpater;

import com.gymproject.common.dto.payment.ProductInfo;
import com.gymproject.common.port.payment.ProductPort;
import com.gymproject.payment.product.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductPort {

    private final ProductService productService;

    @Override
    public ProductInfo getProductInfo(Long productId) {
        return productService.getProductInfo(productId);
    }
}
