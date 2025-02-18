# Use the same base image as pdf-api
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the jar file built by Maven
COPY pdf-service/target/pdf-service.jar app.jar

# Expose the port (adjust if needed)
EXPOSE 8081

# Run the service
ENTRYPOINT ["java", "-jar", "app.jar"]
