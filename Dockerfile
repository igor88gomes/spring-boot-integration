# Steg 1: Bygg Java-projektet med Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Steg 2: Kör applikationen med en smal Java-runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Kör i UTC för konsekventa tider
ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Duser.timezone=UTC"

# Skapa loggmapp och ge skrivbehörighet
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

# Kopiera byggd JAR från byggsteget
COPY --from=build /app/target/integration-0.0.1-SNAPSHOT.jar app.jar

# Starta applikationen
ENTRYPOINT ["java", "-jar", "app.jar"]