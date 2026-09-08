FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/index.html frontend/vite.config.js ./
COPY frontend/src ./src
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend
WORKDIR /backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /backend/target/assistant-1.0.0.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC"
USER 10001:10001
EXPOSE 10000
ENTRYPOINT ["java","-jar","app.jar"]
