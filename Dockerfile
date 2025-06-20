FROM ubuntu:latest AS build

RUN apt-get update
RUN apt-get install openjdk-21-jdk -y

COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean install -U -DskipTests

EXPOSE 8080
ENTRYPOINT ["./mvnw", "spring-boot:run"]
