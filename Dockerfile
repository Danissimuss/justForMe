FROM openjdk:17-jdk-bullseye
WORKDIR /app
COPY /out/artifacts/justForMe_jar/justForMe.jar /app/server.jar
ENTRYPOINT ["java","-Dserver.port=8080", "-jar", "server.jar"]
LABEL authors="Danil"/