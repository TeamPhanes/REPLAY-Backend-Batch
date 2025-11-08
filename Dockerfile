FROM amazoncorretto:21
COPY /build/libs/REPLAY-Backend-Batch-0.0.1-SNAPSHOT.jar /batch.jar
ENTRYPOINT ["java", "-jar", "batch.jar"]