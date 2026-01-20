package com.gymproject.api.config;

import com.gymproject.auth.infrastructure.web.OAuth2FailureHandler;
import com.gymproject.auth.infrastructure.web.OAuth2SuccessHandler;
import com.gymproject.auth.infrastructure.jwt.JwtAuthFilter;
import com.gymproject.auth.infrastructure.jwt.JwtAuthenticationEntryPoint;
import com.gymproject.auth.infrastructure.external.OAuth2UserProviderRouter;
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

                                .requestMatchers(
                                        "/api/v1/auth/**",
                                        "/api/v1/auth/**",
                                        "/oauth/**",
                                        "/book/**",
                                        "/api/v1/classes/**",
                                        "/api/v1/schedules/**",
//                                        "/api/v1/templates/**",
                                        "/api/v1/payments/**",
                                        "/api/v1/admin/products/**",
                                        "/api/webhook/**",
                                        "/api/v1/calendars/**",
                                        "/api/v1/memberships/**",
                                        "/api/v1/sessions/**",
                                        "/users/**").permitAll()
                                .requestMatchers(
                                        "/email/**").permitAll()
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
