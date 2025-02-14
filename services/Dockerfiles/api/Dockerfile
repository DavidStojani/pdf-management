# Use the official OpenJDK image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar
ARG JAR_FILE=pdf-api/target/pdf-api-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Expose port 8080
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
