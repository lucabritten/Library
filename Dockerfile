# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Build-Dateien zuerst: ändern sich selten, Layer bleibt im Cache
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Falls Repo unter Windows ausgecheckt wurde
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-deamon

# Quellcode: ändert sich oft, daher erst jetzt
COPY src/ src/
RUN ./gradlew bootJar --no-deamon

# --- Runtime-Stage ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Egeiner System-User ohne Login und root-Rechte
RUN groupadd --system spring && useradd --system --gid spring spring
# Build result abholen

COPY --from=build --chown=spring:springapp/spring /app/build/libs/app.jar app.jar

# ab hier nicht mehr root
USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]