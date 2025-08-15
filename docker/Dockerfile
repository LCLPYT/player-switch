FROM eclipse-temurin:21-jdk-alpine AS builder

RUN apk add --update curl go && \
    go install github.com/packwiz/packwiz@latest

# setup server
WORKDIR /packwiz

COPY docker/download_mrpack_install.sh .

## download fabric server
RUN ./download_mrpack_install.sh && \
    mkdir -p /server && \
    ./mrpack-install server fabric --server-dir /server --server-file fabric-server-launcher.jar

COPY docker/packwiz .

## install modpack
RUN ~/go/bin/packwiz modrinth export -o mods.mrpack && \
    ./mrpack-install mods.mrpack --server-dir /server

# build project
WORKDIR /project

## use separate layer for gradle and dependencies
COPY ../gradle gradle
COPY ../gradlew build.gradle gradle.properties settings.gradle ./

RUN ./gradlew clean --no-daemon

## build artifact
COPY ../LICENSE .
COPY ../src src

RUN ./gradlew build --no-daemon \
    && rm build/libs/*-sources.jar

# separate stage for runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /server .
COPY docker/run.sh .
COPY --from=builder /project/build/libs/*.jar mods/

VOLUME /app

CMD [ "./run.sh" ]