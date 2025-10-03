# Użyj obrazu JDK jako bazowego
FROM openjdk:21-jdk-slim

# Ustaw zmienną środowiskową dla profilu Spring
ENV SPRING_PROFILES_ACTIVE=local

# Ustaw katalog roboczy
WORKDIR /app

# Skopiuj plik JAR do kontenera
COPY target/Rentowne.jar app.jar

# Eksponuj port aplikacji
EXPOSE 8080

# Ustaw komendę startową dla kontenera
ENTRYPOINT ["java", "-jar", "app.jar"]
