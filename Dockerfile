FROM gradle:9.4-jdk21 AS build

WORKDIR /app

ENV GRADLE_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"

COPY . .

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/friendship-service.jar friendship-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "friendship-service.jar"]