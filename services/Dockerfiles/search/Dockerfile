FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY pdf-outbound-search/target/pdf-outbound-search.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
