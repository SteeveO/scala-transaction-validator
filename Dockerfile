# syntax=docker/dockerfile:1

FROM docker.io/sbtscala/scala-sbt:eclipse-temurin-17.0.14_7_1.10.7_2.13.16 AS builder

WORKDIR /build

COPY project project
COPY build.sbt .
RUN sbt update

COPY app app
COPY conf conf
COPY public public

RUN sbt assembly

FROM docker.io/library/eclipse-temurin:21-jre-alpine AS production

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=builder /build/target/scala-2.13/transaction-validator.jar app.jar
RUN chown -R app:app /app

USER app

EXPOSE 9000

ENTRYPOINT ["java", "-Dplay.server.pidfile.path=/dev/null", "-jar", "app.jar"]
