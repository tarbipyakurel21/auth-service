
FROM eclipse-temurin:17-jdk


WORKDIR /app

COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar


EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
