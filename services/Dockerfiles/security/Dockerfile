FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY pdf-infrastructure-security/target/pdf-infrastructure-security.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
