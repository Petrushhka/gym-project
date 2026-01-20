package com.gymproject.common.port.payment;

import com.gymproject.common.dto.payment.ProductInfo;

public interface ProductPort {

    ProductInfo getProductInfo(Long productId);


}
