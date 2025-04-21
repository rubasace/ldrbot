FROM franky1/tesseract:5.5.0 as tesseract

FROM eclipse-temurin:21-jre-jammy

#Copy Tesseract
COPY --from=tesseract /usr/local/bin/tesseract /usr/local/bin/tesseract
COPY --from=tesseract /usr/local/lib/libtesseract.so* /usr/local/lib/

WORKDIR /app

COPY tessdata/* /usr/share/tessdata/

COPY target/*.jar /app/linkedin-games-tracker.jar

USER 1000:999

ENTRYPOINT ["java", "-jar", "linkedin-games-tracker.jar"]