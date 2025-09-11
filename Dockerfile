# Steg 1: Bygg Java-projektet med Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Ladda ner beroenden cache-vänligt (bättre cache-träffar)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline

# 2) Bygg koden (utan tester här; tester körs i CI)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# Steg 2: Kör applikationen med en smal Java-runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Kör i UTC för konsekventa tider (JVM)
ENV JAVA_TOOL_OPTIONS=""
ENV JAVA_TOOL_OPTIONS="--add-opens java.base/java.io=ALL-UNNAMED -Duser.timezone=UTC"

# Skapa loggmapp och ge skrivbehörighet
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

# Kopiera byggd JAR från byggsteget
COPY --from=build /app/target/integration-0.0.1-SNAPSHOT.jar app.jar

# Starta applikationen
ENTRYPOINT ["java", "-jar", "app.jar"]