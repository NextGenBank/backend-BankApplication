FROM ubuntu:latest AS build

RUN apt-get update && \
    apt-get install openjdk-21-jdk maven -y

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
