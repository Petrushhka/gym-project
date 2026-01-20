package com.gymproject.payment.product.domain.entity;

import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import com.gymproject.payment.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("정상적인 데이터로 상품을 생성하면 ACTIVE 상태로 생성된다.")
    void create_product_success() {
        // given
        String name = "여름 맞이 3개월권";
        String code = "MEMBERSHIP_3M";
        Long price = 150000L;
        ProductCategory category = ProductCategory.MEMBERSHIP;

        // when
        Product product = Product.createProduct(name, code, price, category);

        // then
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getCode()).isEqualTo(code);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE); // 기본값 확인
    }

    @Test
    @DisplayName("가격이 0원 미만이면 예외가 발생한다.")
    void create_fail_negative_price() {
        // given
        Long invalidPrice = -100L;

        // when & then
        assertThatThrownBy(() ->
                Product.createProduct("상품", "CODE", invalidPrice, ProductCategory.SESSION)
        )
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PRODUCT_INVALID_PRICE");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("상품 이름이 없거나 공백이면 예외가 발생한다.")
    void create_fail_invalid_name(String invalidName) {
        // when & then
        assertThatThrownBy(() ->
                Product.createProduct(invalidName, "CODE", 1000L, ProductCategory.SESSION)
        )
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_PRODUCT_DATA");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("상품 코드가 없거나 공백이면 예외가 발생한다.")
    void create_fail_invalid_code(String invalidCode) {
        // when & then
        assertThatThrownBy(() ->
                Product.createProduct("이름", invalidCode, 1000L, ProductCategory.SESSION)
        )
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_PRODUCT_DATA");
    }
}

@Nested
@DisplayName("2. 비즈니스 로직(삭제) 테스트")
class BusinessLogicTest {

    @Test
    @DisplayName("정상적인 상품을 삭제하면 상태가 INACTIVE로 변경된다.")
    void delete_success() {
        // given
        Product product = Product.createProduct("삭제할 상품", "DEL_001", 10000L, ProductCategory.SESSION);

        // when
        product.delete();

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("이미 삭제된 상품을 다시 삭제하려 하면 예외가 발생한다.")
    void delete_fail_already_deleted() {
        // given
        Product product = Product.createProduct("삭제된 상품", "DEL_002", 10000L, ProductCategory.SESSION);
        product.delete(); // 1차 삭제 (ACTIVE -> INACTIVE)

        // when & then (2차 삭제 시도)
        assertThatThrownBy(() -> product.delete())
                .isInstanceOf(ProductException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PRODUCT_ALREADY_DELETED");
    }
}

