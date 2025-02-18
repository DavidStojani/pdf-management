# Use the official OpenJDK image
FROM openjdk:21-ea-1-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar
ARG JAR_FILE=pdf-api/target/pdf-api-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Expose ports for the application and debugger
EXPOSE 8080 5005

# Enable debugging
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]
