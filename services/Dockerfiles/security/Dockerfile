FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY pdf-security/target/pdf-security.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
