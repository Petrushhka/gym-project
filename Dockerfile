# 빌드 단계(gradle)
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /build

# 의존성 캐싱을 위해 설정파일만 먼저 복사
COPY build.gradle settings.gradle /build/
RUN gradle build-x test --parallel --continue > /dev/null 2>&1 || true

# 소스코드 복사 및 실제 빌드
COPY . /build
RUN gradle build -x test --parallel

## 실행 단계(JDK)
FROM amazoncorretto:17-alpine
WORKDIR /app

# 빌드 결과물(jar) 복사
COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar