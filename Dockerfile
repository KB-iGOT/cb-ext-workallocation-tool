FROM openjdk:17-slim

# Install necessary dependencies
RUN apt-get update \
    && apt-get install -y \
        curl \
        libxrender1 \
        libjpeg62-turbo \
        fontconfig \
        libxtst6 \
        xfonts-75dpi \
        xfonts-base \
        xz-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY workallocation-0.0.1-SNAPSHOT.jar /opt/
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -jar /opt/workallocation-0.0.1-SNAPSHOT.jar"]
