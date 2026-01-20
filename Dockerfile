# 빌드 단계(gradle)
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /build

# 의존성 캐싱을 위해 설정파일만 먼저 복사
COPY build.gradle settings.gradle /build/
RUN gradle build-x test --parallel --continue > /dev/null 2>&1 || true

# 소스코드 복사 및 실제 빌드
COPY . /build
RUN gradle build -x test --parallel

# 껍데기(plain) 빼고 진짜만 app.jar로 저장
RUN find api/build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} /build/app.jar \;

## 실행 단계(JDK)
FROM amazoncorretto:17-alpine
WORKDIR /app

# 빌드 결과물(jar)
COPY --from=builder /build/app.jar app.jar

# 실행 명렁어
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

