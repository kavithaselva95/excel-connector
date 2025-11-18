FROM openjdk:17-jre-slim
WORKDIR /app
COPY build/libs/excel-connector-1.0.0.jar /app/excel-connector.jar
COPY test-data/ /app/test-data/
ENV TEST_DATA_DIR=/app/test-data
CMD ["java", "-jar", "/app/excel-connector.jar", "/app/test-data"]