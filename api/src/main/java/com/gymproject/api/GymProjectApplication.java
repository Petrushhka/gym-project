package com.gymproject.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "com.gymproject") // 스캔할 범위를 지정함.
@ComponentScan(basePackages = "com.gymproject") // 서비스, 컨트롤러 스캔
@EnableJpaRepositories(basePackages = "com.gymproject") // 레포지토리 스캔
@EntityScan(basePackages = "com.gymproject")
@EnableScheduling // 스케줄러 엔진 돌아가게함
public class GymProjectApplication {

    @PostConstruct
    public void init(){
        // 애플리케이션이 돌아가는 JVM의 기본 타임존을 UTC로 고정함.
        // OffsetDateTime.now()는 어느 리전에서 돌아가든 영국표준 시각을 반환함
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(GymProjectApplication.class, args);
    }
}
