# Container image of the application. Build the jar first: ./mvnw -q package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 sovereign && mkdir -p /app/logs/audit && chown -R 10001 /app/logs
COPY target/sovereign-persistence-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
