# Steg 1: Bygg Java-projektet med Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Förvärm Maven-cache genom en "torr" package (utan tester/javadoc)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -Dmaven.javadoc.skip=true package && rm -rf target

# 2) Bygg koden (utan tester här; tester körs i CI)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# Steg 2: Kör applikationen med en smal Java-runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Kör i UTC för konsekventa tider (JVM)
ENV JAVA_TOOL_OPTIONS=""

# HÅRDNING: kör som icke-root
# Skapa systemanvändare/grupp utan inloggningsshell
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --system --gid ${APP_GID} app \
 && useradd  --system --no-create-home --uid ${APP_UID} --gid ${APP_GID} \
             --shell /usr/sbin/nologin app

# Kopiera byggd JAR från byggsteget
COPY --from=build --chown=app:app /app/target/integration-0.0.1-SNAPSHOT.jar app.jar

# Kör som icke-root
USER app:app

# Starta applikationen
ENTRYPOINT ["java", "--add-opens=java.base/java.io=ALL-UNNAMED", "-Duser.timezone=UTC", "-jar", "app.jar"]