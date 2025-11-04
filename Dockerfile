FROM amazoncorretto:21
COPY /build/libs/REPLAY-Backend-Batch-0.0.1-SNAPSHOT.jar /opensearch-sync.jar
ENTRYPOINT ["java", "-jar", "opensearch-sync.jar"]