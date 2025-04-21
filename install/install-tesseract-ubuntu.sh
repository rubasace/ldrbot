#!/bin/sh
set -e

# GPG key fingerprint for alex-p PPA
KEY=3A788ED11EFFCAF9

# Install minimal tools
apt-get update && apt-get install -y --no-install-recommends \
    curl gnupg ca-certificates

# Import and store GPG key
gpg --keyserver keyserver.ubuntu.com --recv-keys $KEY
gpg --export $KEY | gpg --dearmor -o /usr/share/keyrings/tesseract-ocr5.gpg

# Add Tesseract PPA
echo "deb [signed-by=/usr/share/keyrings/tesseract-ocr5.gpg] \
http://ppa.launchpad.net/alex-p/tesseract-ocr5/ubuntu jammy main" \
> /etc/apt/sources.list.d/tesseract-ocr5.list

# Install Tesseract 5 core only
apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr

# Clean up to reduce layer size
rm -rf /var/lib/apt/lists/*