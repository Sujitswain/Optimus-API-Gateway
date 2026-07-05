# Multi-stage build for Spring Boot services
FROM maven:3.9-eclipse-temurin-19 AS builder

WORKDIR /build

# Copy the entire project
COPY . .

# Build the specific service (passed as build arg)
ARG SERVICE_NAME
RUN mvn clean package -DskipTests -pl ${SERVICE_NAME}/${SERVICE_NAME} -am

# Runtime stage
FROM eclipse-temurin:19-jre

WORKDIR /app

# Copy the built jar from builder
ARG SERVICE_NAME
COPY --from=builder /build/${SERVICE_NAME}/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
