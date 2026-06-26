#FROM gradle:8.14-jdk21 AS build
#WORKDIR /app
#COPY . .
#RUN gradle :portfolio-service:bootJar --no-daemon
#
#FROM amazoncorretto:21-alpine
#WORKDIR /app
#RUN apk add --no-cache curl
#COPY --from=build /app/portfolio-service/build/libs/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]

FROM amazoncorretto:21-alpine
WORKDIR /app
RUN apk add --no-cache curl
RUN curl -L -o /app/otel-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.29.0/opentelemetry-javaagent.jar
COPY portfolio-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-javaagent:/app/otel-javaagent.jar", "-jar", "app.jar"]
