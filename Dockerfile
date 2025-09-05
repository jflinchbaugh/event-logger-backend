FROM debian:unstable
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update -y \
    && apt-get upgrade -y \
    && apt-get install -y openjdk-24-jre-headless time

RUN mkdir -p /app

WORKDIR /app

COPY target/*-standalone.jar app.jar
COPY entrypoint.sh entrypoint.sh

CMD ./entrypoint.sh