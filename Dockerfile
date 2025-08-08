# Steg 1: Bygg Java-projektet med Maven
FROM docker.io/library/maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Steg 2: Kör applikationen med en smal Java-runtime
FROM docker.io/library/eclipse-temurin:17-jdk
WORKDIR /app

# Ställ in tidszon för korrekt loggrotation
ENV TZ=Europe/Stockholm
RUN apt-get update && apt-get install -y tzdata && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /app/target/integration-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
