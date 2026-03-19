# Stage 1 — Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Stage 2 — Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S tradeflow && adduser -S tradeflow -G tradeflow

COPY --from=builder /app/target/*.jar app.jar

RUN chown tradeflow:tradeflow app.jar

USER tradeflow

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]