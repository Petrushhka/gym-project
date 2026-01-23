package com.gymproject.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TreeMap;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("1. 회원 및 인증")
                .packagesToScan("com.gymproject.auth", "com.gymproject.user.api")
                .pathsToMatch("/api/v1/users/**", "/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi classApi() {
        return GroupedOpenApi.builder()
                .group("2. 수업 스케줄 관리")
                .packagesToScan("com.gymproject.classmanagement", "com.gymproject.readmodel")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("3. 결제 시스템(Stripe)")
                .packagesToScan("com.gymproject.payment", "com.gymproject.user.api")
                .pathsToMatch("/api/v1/payments/**", "/api/v1/memberships/**", "/api/v1/sessions/**"
                , "/api/v1/admin/products/**", "/api/webhook/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("4. 예약 관리")
                .packagesToScan("com.gymproject.booking", "com.gymproject.readmodel")
                .build();
    }

    // [중요] 토큰을 넣는 기능
    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 인증 정보를 어떤 이름으로 부를지 정의 (Key 값)
        String securitySchemeName = "JWT 인증";

        return new OpenAPI()
                // 2. API 문서의 기본 정보 설정 (제목, 설명, 버전)
                .info(new Info()
                        .title("Gym Project API") // 문서 제목
                        .description("""
                                <h3>호주 브리즈번 기반 개인 헬스장 예약 시스템 API 명세서</h3>
                                <p>본 프로젝트는 JWT 기반 인증을 사용합니다.</p>
                                <p>개발 편의를 위해 응답 및 설정은 현재 한국어로 설정되어 있습니다.</p>
                                <br/>
                                 <strong>테스트용 트레이너 계정</strong><br/>
                                - email:<code>trainer@naver.com</code></br>
                                - PW: <code>Trainer0189!</code><br/>
                                <br/>
                                <strong>정회원 계정</strong><br/>
                                - email:<code>member@naver.com</code></br>
                                - PW: <code>Member0189!</code><br/>
                                
                                <br/>
                                <strong>테스트 방법</strong><br/>
                                1. <code>/api/v1/auth/login</code> 에서 위 계정으로 로그인<br/>
                                2. 응답받은 <strong>AccessToken</strong> 복사<br/>
                                3. 우측 상단 <strong>Authorize</strong> 버튼 클릭 후 <code>Value {Token}</code> 입력
                                """
                        ) // 상세 설명
                        .version("v.1.0.0")) // API 버전

                // 3. 전역 보안 요구사항 설정
                // 모든 API 호출 시 기본적으로 위에서 정의한 'JWT 인증' 방식을 사용하도록 지정함
                .addSecurityItem(new SecurityRequirement().addList("JWT 인증"))

                // 4. 보안 방식의 구체적인 정의 (Components 등록)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName) // 보안 스킴의 이름
                                .type(SecurityScheme.Type.HTTP) // HTTP 방식의 보안
                                .scheme("bearer") // Bearer 방식을 사용 (Bearer {token})
                                .bearerFormat("JWT"))); // 토큰의 형식이 JWT임을 명시
    }

    /*
        모든 API를 (summary = 1,2,3..)의 순서대로 정렬하는 설정임
     */
    @Bean
    public GlobalOpenApiCustomizer sortOperationsBySummary() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            Paths sortedPaths = new Paths();

            // 1. TreeMap을 사용하여 (태그 + 요약 + 경로) 조합으로 정렬 키 생성
            // TreeMap은 키값을 기준으로 자동 오름 차순 정렬
            TreeMap<String, String> sortedKeys = new TreeMap<>();

            paths.forEach((path, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    // 태그명과 Summary를 조합하여 정렬(예: "1. 회원 및 인증")
                    String tag = (operation.getTags() != null && !operation.getTags().isEmpty())
                            ? operation.getTags().get(0) : "Default";
                    String summary = (operation.getSummary() != null) ? operation.getSummary() : "";

                    // 정렬 키: 태크_Summary_HTTPMethod_Path
                    String sortKey = tag + "_" + summary + "_" + path;
                    sortedKeys.put(sortKey, path);
                });
            });

            // 2. 정렬된 순서대로 새로운 Paths 객체에 다시 담기
            sortedKeys.values().forEach(path -> {
                if (paths.containsKey(path)) {
                    sortedPaths.addPathItem(path, paths.get(path));
                }
            });

            // 3. 최종적으로 정렬된 경로들로 교체
            openApi.setPaths(sortedPaths);
        };
    }

}
