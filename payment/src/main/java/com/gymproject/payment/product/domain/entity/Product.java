package com.gymproject.payment.product.domain.entity;

import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import com.gymproject.payment.product.exception.ProductErrorCode;
import com.gymproject.payment.product.exception.ProductException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "PRODUCT_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "name", nullable = false)
    private String name; // 화면 표시용 (ex: 여름 맞이 1개월권)

    @Column(name = "price", nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ProductCategory category; // 리스너 구분용 MEMBERSHIP , SESSION

    @Column(name = "code", nullable = false)
    private String code; // MEMBERSHIP_3 , PT_10_SESSION 등

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    private ProductStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    public Product(String name, ProductCategory category,
                   Long price, String code, ProductStatus status) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.code = code;
        this.status = status;
    }

    public static Product createProduct(String name, String code,Long price, ProductCategory category) {
        validateInput(price, name, code);
        return Product.builder()
                .name(name)
                .code(code)
                .price(price)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    public void delete(){
        if(this.status == ProductStatus.INACTIVE){
            throw new ProductException(ProductErrorCode.PRODUCT_ALREADY_DELETED);
        }
        this.status = ProductStatus.INACTIVE;
    }

    private static void validateInput(Long price, String name, String code){
        // [검증] 가격은 0원 이상이어야 함
        if (price < 0) {
            throw new ProductException(ProductErrorCode.PRODUCT_INVALID_PRICE);
        }
        // [검증] 이름과 코드는 필수
        if (!StringUtils.hasText(name) || !StringUtils.hasText(code)) {
            throw new ProductException(ProductErrorCode.INVALID_PRODUCT_DATA);
        }
    }
}
