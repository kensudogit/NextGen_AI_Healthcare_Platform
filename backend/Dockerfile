FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /app/storage/dicom
COPY --from=build /app/target/platform-api-1.0.0.jar app.jar
EXPOSE 8010
ENTRYPOINT ["java", "-jar", "app.jar"]
