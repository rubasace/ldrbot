FROM eclipse-temurin:21-jre-jammy

# TODO migrate to tesseract 5 here and on pipeline
# TODO think of using installed language data
RUN apt update && \
    apt install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY tessdata/* /usr/share/tessdata/

COPY target/*.jar /app/linkedin-games-tracker.jar

USER 1000:999

ENTRYPOINT ["java", "-jar", "linkedin-games-tracker.jar"]