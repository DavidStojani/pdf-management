FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY pdf-search/target/pdf-search.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
