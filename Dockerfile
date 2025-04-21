FROM eclipse-temurin:21-jre-jammy

#Install Tesseract
COPY install/install-tesseract.sh /tmp/install-tesseract.sh
RUN chmod +x /tmp/install-tesseract.sh && /tmp/install-tesseract.sh && rm /tmp/install-tesseract.sh

WORKDIR /app

COPY tessdata/* /usr/share/tessdata/

COPY target/*.jar /app/linkedin-games-tracker.jar

USER 1000:999

ENTRYPOINT ["java", "-jar", "linkedin-games-tracker.jar"]