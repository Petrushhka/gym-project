package com.gymproject.payment.api;

import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.payment.application.dto.ProductResponse;
import com.gymproject.payment.product.application.dto.CreateProductRequest;
import com.gymproject.payment.product.application.service.ProductService;
import com.gymproject.payment.product.domain.entity.Product;
import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "1. 상품 관리", description = "판매 상품 관리")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 조회
    /**
     * 3. 상품 목록 조회 (통합 검색)
     *
     * [사용 예시]
     * 1. 전체 조회 (관리자): GET /api/v1/products
     * 2. 판매중인 것만 (유저): GET /api/v1/products?status=ACTIVE
     * 3. 멤버십만 (관리자): GET /api/v1/products?category=MEMBERSHIP
     * 4. 판매중인 멤버십 (유저): GET /api/v1/products?category=MEMBERSHIP&status=ACTIVE
     */
    @Operation(
            summary = "1. 상품 목록 조회 (필터링)",
            description = """
            시스템에 등록된 상품 목록을 조회합니다. 카테고리와 상태를 조합하여 검색할 수 있습니다.
            
            CATEGORY: MEMBERSHIP(기간권), SESSION(횟수권)
            STATUS: ACTIVE(판매중), INACTIVE(판매중단)
            
            [예시: 판매중인 멤버십만 조회]
            `GET /api/v1/admin/products?category=MEMBERSHIP&status=ACTIVE`
            """
    )
    @GetMapping
    public ResponseEntity<CommonResDto<List<ProductResponse>>> getProducts(
            @RequestParam(required = false)  ProductCategory category,
            @RequestParam(required = false) ProductStatus status
    ) {
        List<Product> products = productService.searchProducts(category, status);

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::create)
                .toList();

        return ResponseEntity.ok(
                CommonResDto.success(200, "조회 성공", responses)
        );
    }

    @Operation(summary = "2. 새로운 상품 등록", description = """
            멤버십(기간권) 또는 세션(횟수권) 상품을 시스템에 등록합니다.
            현재는 서버에서 상품을 정해놓은 상태이지만, 다음 버전에서 상품과 관련된 도메인을 확장이 필요합니다.          
           """)
    @PostMapping
    public ResponseEntity<CommonResDto<ProductResponse>> createProduct(@RequestBody CreateProductRequest product) {
        ProductResponse response = productService.createProduct(product);

        return ResponseEntity.ok(
                CommonResDto.success(200, "상품이 등록되었습니다.", response)
        );
    }

    @Operation(summary = "3. 상품 삭제", description = "특정 상품을 삭제합니다. (논리 삭제)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<CommonResDto<ProductResponse>> deleteProduct(@PathVariable Long productId) {
        ProductResponse response = productService.deleteProduct(productId);

        return ResponseEntity.ok(
                CommonResDto.success(200, "성공적으로 삭제했습니다.", response)
        );
    }
}
