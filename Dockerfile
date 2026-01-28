# syntax=docker/dockerfile:1
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app
COPY build.gradle gradle.properties settings.gradle ./
COPY src ./src
RUN gradle --no-daemon build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/build/libs/drone-delivery-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
