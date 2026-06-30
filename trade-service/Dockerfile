#FROM gradle:8.14-jdk21 AS build
#WORKDIR /app
#COPY . .
#RUN gradle :trade-service:bootJar --no-daemon
#
#FROM amazoncorretto:21-alpine
#WORKDIR /app
#RUN apk add --no-cache curl
#COPY --from=build /app/trade-service/build/libs/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]

FROM amazoncorretto:21-alpine
WORKDIR /app
RUN apk add --no-cache curl
RUN mkdir -p /opt/otel && curl -L -o /opt/otel/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.29.0/opentelemetry-javaagent.jar
COPY trade-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]