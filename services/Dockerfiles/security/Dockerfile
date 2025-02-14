FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY pdf-security/target/pdf-security.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
