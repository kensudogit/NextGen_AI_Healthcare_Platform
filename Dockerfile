# NextGen Healthcare Platform — Backend (Spring Boot)
# Railway / クラウドデプロイ用（リポジトリルートからビルド）

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /app/storage/dicom
COPY --from=build /app/target/platform-api-1.0.0.jar app.jar

# Railway は PORT を注入する
ENV SERVER_PORT=8010
EXPOSE 8010

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8010}"]
