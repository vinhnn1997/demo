# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
ARG COMMON_MAVEN_REPOSITORY_URL
ENV COMMON_MAVEN_REPOSITORY_URL=${COMMON_MAVEN_REPOSITORY_URL}
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml,required=false \
	test -n "$COMMON_MAVEN_REPOSITORY_URL" && \
	mvn -B -f taxpayer-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/taxpayer-service/target/taxpayer-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
