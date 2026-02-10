# Use the official OpenJDK image
FROM openjdk:21-ea-1-jdk-slim

# Install Tesseract OCR and necessary language data (e.g., English + German)
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-deu && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the built jar
#ARG JAR_FILE=pdf-inbound-api/target/pdf-inbound-api-1.0-SNAPSHOT.jar
#COPY ${JAR_FILE} app.jar

# Expose ports for the application and debugger
EXPOSE 8080 5005

# Enable debugging
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]
