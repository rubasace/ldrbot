FROM franky1/tesseract:5.5.0 as tesseract

RUN mkdir -p /tesseract-dist && \
    ldd /usr/local/bin/tesseract | awk '{print $3}' | grep -E '^/' | xargs -I{} cp --parents {} /tesseract-dist/ && \
    ldd /usr/local/lib/libtesseract.so | awk '{print $3}' | grep -E '^/' | xargs -I{} cp --parents {} /tesseract-dist/

FROM eclipse-temurin:21-jre-jammy

#Copy Tesseract
COPY --from=tesseract /tesseract-dist /tesseract-dist

RUN ln -sfn /usr/lib /lib

RUN apt update && apt install -y rsync && \
    rsync -a --ignore-existing /tesseract-dist/lib/ /usr/lib/ && \
    rsync -a --ignore-existing /tesseract-dist/usr/ /usr/ &&\
    apt remove -y rsync && \
    rm -rf /tesseract-dist

WORKDIR /app

COPY tessdata/* /usr/share/tessdata/

COPY target/*.jar /app/linkedin-games-tracker.jar

USER 1000:999

ENV JDK_OPTIONS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=75 -XX:MaxRAMPercentage=85 -XX:MaxJavaStackTraceDepth=15"

ENTRYPOINT ["java", "-jar", "linkedin-games-tracker.jar"]