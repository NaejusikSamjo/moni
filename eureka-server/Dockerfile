FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"
RUN gradle :eureka-server:bootJar --no-daemon --max-workers=2

FROM amazoncorretto:21-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/eureka-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
