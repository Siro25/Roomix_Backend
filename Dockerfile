# syntax=docker/dockerfile:1.7

ARG SERVICE

FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /workspace

COPY ${SERVICE}/mvnw ./mvnw
COPY ${SERVICE}/.mvn ./.mvn
COPY ${SERVICE}/pom.xml ./pom.xml

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY ${SERVICE}/src ./src
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S roomix \
    && adduser -S roomix -G roomix

WORKDIR /app
COPY --from=build --chown=roomix:roomix /workspace/target/*.jar ./application.jar

USER roomix
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
