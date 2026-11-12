FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system newsroom && useradd --system --gid newsroom --no-create-home newsroom
WORKDIR /app
COPY --from=build --chown=newsroom:newsroom /build/target/bracu-insiders-1.0.0.jar /app/newsroom.jar
USER newsroom
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/newsroom.jar"]
