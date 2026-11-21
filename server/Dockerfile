# Runtime image only: the app is built on the host first (the repo's Gradle build needs the
# Android SDK), so no source code goes into the image.
#   ./gradlew :server:installDist && docker build -t liftly-server server/
# eclipse-temurin is multi-arch, so the same file builds for the Oracle Ampere (arm64) VM.
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 liftly
COPY build/install/server /app
USER liftly

EXPOSE 8080
ENTRYPOINT ["/app/bin/server"]
