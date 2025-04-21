FROM eclipse-temurin:21-jre-jammy

# TODO think of using language data
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/*.jar /app/linkedin-games-tracker.jar

ENTRYPOINT ["java", "-jar", "linkedin-games-tracker.jar"]