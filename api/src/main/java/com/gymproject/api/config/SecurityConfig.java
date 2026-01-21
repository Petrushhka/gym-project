package com.gymproject.api.config;

import com.gymproject.auth.infrastructure.external.OAuth2UserProviderRouter;
import com.gymproject.auth.infrastructure.jwt.JwtAuthFilter;
import com.gymproject.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.gymproject.auth.infrastructure.web.OAuth2FailureHandler;
import com.gymproject.auth.infrastructure.web.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuth2UserProviderRouter oAuth2UserProviderRouter;
    private final OAuth2SuccessHandler successHandler;
    private final OAuth2FailureHandler failureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a ->
                        a.requestMatchers(
                                        // swagger 관련
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/swagger-resources/**").permitAll()

                                // [공통 & 이메일 관련] - 인증 불필요
                                .requestMatchers(POST, "/api/v1/auth/emails/codes").permitAll()        // 코드 발송
                                .requestMatchers(POST, "/api/v1/auth/emails/verifications").permitAll() // 가입용 코드 확인
                                .requestMatchers(POST, "/api/v1/auth/passwords/verification-code").permitAll() // 비번찾기 코드 확인
                                .requestMatchers(POST, "/api/v1/auth/emails/search").permitAll()       // 이메일 찾기
                                // [회원가입 & 로그인 관련] - 인증 불필요
                                .requestMatchers(POST, "/api/v1/auth/signup").permitAll()        // 일반 가입
                                .requestMatchers(POST, "/api/v1/auth/signup/social").permitAll() // 소셜 가입(추가정보)
                                // [로그인 & 토큰 갱신] - 인증 불필요
                                .requestMatchers(POST, "/api/v1/auth/login").permitAll()
                                .requestMatchers(POST, "/api/v1/auth/refresh").permitAll()
                                // [비밀번호 재설정 (비로그인)] - 인증 불필요
                                .requestMatchers(POST, "/api/v1/auth/passwords/reset").permitAll()
                                //[예약 진행] - 정회원용
                                .requestMatchers(POST, "/api/v1/bookings").hasAnyRole("MEMBER", "ADMIN")
                                // [예약 승인/거절] - 트레이너/관리자용
                                .requestMatchers(POST, "/api/v1/bookings/*/status").hasAnyRole("TRAINER", "ADMIN")
                                // [정기 수업 예약 / 취소] - 정회원용
                                .requestMatchers(POST, "/api/v1/bookings/curriculums/*").hasAnyRole("TRAINER", "ADMIN", "MEMBER")
                                .requestMatchers(POST, "/api/v1/bookings/schedules/*").hasAnyRole("TRAINER", "ADMIN", "MEMBER")
                                .requestMatchers(DELETE, "/api/v1/bookings/curriculums/*").hasAnyRole("TRAINER", "ADMIN", "MEMBER")
                                // [예약 막기/해제] - 트레이너만/관리자용
                                .requestMatchers("/api/v1/time-offs/**").hasAnyRole("TRAINER", "ADMIN")
                                // [수업 템플릿 생성/ 삭제]
                                .requestMatchers("/api/v1/templates/**").hasAnyRole("TRAINER", "ADMIN")
                                // [정기 수업 생성/ 삭제]
                                .requestMatchers("/api/v1/classes/**").hasAnyRole("TRAINER", "ADMIN")
                                // [개별 수업 관리]
                                .requestMatchers("/api/v1/schedules/**").hasAnyRole("TRAINER", "ADMIN")
                                // [상품등록]
                                .requestMatchers("/api/v1/admin/products/**").hasAnyRole("TRAINER", "ADMIN")
                                // [결제 환불]
                                .requestMatchers(POST,"/api/v1/payments/refund").hasAnyRole("MEMBER", "ADMIN")
                                // [결제 리다이렉터 경로 - 추후 프론트 주소로 옮겨야함]
                                .requestMatchers(GET,"/api/v1/payments/payment/success").permitAll()
                                .requestMatchers(GET,"/api/v1/payments/payment/cancel").permitAll()
                                // [stripe 웹훅]
                                .requestMatchers(POST,"/api/webhook/**").permitAll()
                                // [일정 조회]
                                .requestMatchers(GET,"/api/v1/calendars/**").permitAll()
                                .anyRequest().authenticated())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .oauth2Login(customConfigurer -> customConfigurer
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .userInfoEndpoint(endpointConfig ->
                                endpointConfig.userService(oAuth2UserProviderRouter)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Credentials랑 같이 쓸 수 있음
        configuration.setAllowedOriginPatterns(List.of("*"));

        configuration.setAllowedMethods(List.of("*")); // GET, POST 모두 허용
        configuration.setAllowedHeaders(List.of("*")); // 헤더 다 허용
        configuration.setAllowCredentials(true); // 쿠키, 인증정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
