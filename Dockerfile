FROM gradle:8.13-jdk21 AS builder
WORKDIR /app

COPY . .

RUN gradle jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/saures-backend-1.0.0.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]