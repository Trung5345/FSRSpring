# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY scripts/mvnw scripts/mvnw
COPY pom.xml pom.xml
RUN chmod +x scripts/mvnw
RUN ./scripts/mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./scripts/mvnw -q -DskipTests clean package

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=default

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
