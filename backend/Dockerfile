# Stage 1: Build
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1024m -Dorg.gradle.workers.max=2"
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon
COPY src ./src
RUN gradle bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVER_PORT=8080
ENV SERVER_PORT=${SERVER_PORT}

# Install Tesseract OCR and English language pack, create unprivileged user lingua (UID 10001)
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng && \
    addgroup -g 10001 -S lingua && \
    adduser -u 10001 -S lingua -G lingua && \
    mkdir -p /app/data/curriculum /tmp && \
    chown -R lingua:lingua /app /tmp

COPY --from=build --chown=lingua:lingua /app/build/libs/*.jar app.jar

USER lingua:lingua
EXPOSE ${SERVER_PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
