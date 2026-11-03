# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S billing && adduser -S billing -G billing
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R billing:billing /app
USER billing
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
