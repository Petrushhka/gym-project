package com.gymproject.support;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(()-> {
                    // 1. 실제 요청을 만드는 팩토리 생성
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

                    // 2. 팩토리에서 직접 타임아웃 설정
                    factory.setConnectTimeout(Duration.ofSeconds(5)); // 연결 5초 타임아웃
                    factory.setReadTimeout(Duration.ofSeconds(5)); // 읽기 5초 타임아웃
                    return factory;
                })
                .build();
    }
}

// setReadTimeout, setConnectTimeout 지원중단
