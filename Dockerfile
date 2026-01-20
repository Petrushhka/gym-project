# 빌드 단계(gradle)
FROM gradle:jdk-17-alpine as builder
WORKDIR /build

# 의존성 캐싱을 위해 설정파일만 먼저 복사
COPY build.gradle settings.gradle /build/
RUN gradle build-x test --parallel --continue > /dev/null 2>&1 || true

# 소스코드 복사 및 실제 빌드
COPY . /build
RUN gradle build -x test --parallel

## 실행 단계(JDK)
FROM openjdk:17-alpine
WORKDIR /app

# 빌드 결과물(jar) 복사
COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar

# 실행 명령어(prod 프로파일 활성화)
ENTRYPOINT ["java", "-jar", "_Dspring.profile.active=prod", "app.jar"]