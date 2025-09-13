FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY build/libs/plugin-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "app.jar" ]