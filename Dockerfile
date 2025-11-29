# Etapa 1: construir la app con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Etapa 2: ejecutar la app
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia el .jar construido en la primera etapa
COPY --from=build /app/target/*.jar app.jar

# Render define $PORT, tu app debe usarlo
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]