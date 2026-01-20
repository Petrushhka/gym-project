package com.gymproject.payment.product.infrastructure.persistence;

import com.gymproject.payment.product.domain.entity.Product;
import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
    SELECT p FROM Product p WHERE 
    (:category IS NULL OR p.category = :category)
    AND (:status IS NULL OR p.status = :status)
""")
   List<Product> searchProducts(
           @Param("category")ProductCategory category,
           @Param("status")ProductStatus status);

    Optional<Product> findById(Long id);

    boolean existsByCodeAndStatus(String code, ProductStatus status);
}

/*
        SELECT p FROM Product p WHERE
    (:category IS NULL OR p.category = :category)
    AND (:status IS NULL OR p.status = :status)

    "파라미터가 비어있으면(null)이면 전부 통과시키고, 값이 있으면 같은것만 조회"

      A OR B 문법: 둘 중 하나만 참이어도 전체가 참임.

      category가 null이면 TRUE 가 되어 뒤 조건은 안보게됨

      Short-Circuit Evaluation( || )
 */
