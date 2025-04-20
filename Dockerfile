FROM eclipse-temurin:21-jre-alpine

# Install tesseract
RUN apk add --no-cache tesseract-ocr

# Create app directory
WORKDIR /app

# Copy pre-built JAR (adjust name if needed)
COPY build/libs/*.jar app.jar

# Set environment variable so JNA finds native libs
ENV JNA_LIBRARY_PATH=/usr/lib

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]