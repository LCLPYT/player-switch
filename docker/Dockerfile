FROM eclipse-temurin:21-jdk-alpine AS builder

RUN apk add --update curl go && \
    go install github.com/packwiz/packwiz@latest

WORKDIR /deps

COPY docker/download_mrpack_install.sh ./

RUN ./download_mrpack_install.sh && mv mrpack-install /usr/bin/mrpack-install

# setup server
WORKDIR /packwiz

## download fabric server
RUN mkdir -p /server && \
    mrpack-install server fabric --server-dir /server --server-file fabric-server-launcher.jar

COPY docker/packwiz .

## install modpack
RUN ~/go/bin/packwiz modrinth export -o mods.mrpack && \
    mrpack-install mods.mrpack --server-dir /server

# build project
WORKDIR /project

## use separate layer for gradle and dependencies
COPY gradle gradle
COPY gradlew build.gradle gradle.properties settings.gradle ./

RUN ./gradlew clean --no-daemon

## build artifact
COPY LICENSE .
COPY src src

RUN ./gradlew build --no-daemon \
    && rm build/libs/*-sources.jar

# separate stage for runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# copy project-specific files
COPY docker/run.sh docker/reset.sh ./

# copy server files, mod artifact and server icon
COPY --from=builder /server .
COPY --from=builder /project/build/libs/*.jar mods/
COPY --from=builder /project/src/main/resources/assets/player-switch/icon_64px.png server-icon.png

RUN chmod -R o+rwx /app

# /app directory will be copied to a volume for persistence
VOLUME /app

EXPOSE 25565/tcp

# run run wrapper script by default
CMD [ "./run.sh" ]