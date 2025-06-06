FROM eclipse-temurin:21-jre
COPY target/KafkaConsumer1_PostgreSQL_OpenSearch.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]