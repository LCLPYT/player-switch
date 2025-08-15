#!/usr/bin/env sh

if [ "$EULA" = "true" ]; then
    echo "eula=true" > eula.txt
fi

java -jar fabric-server-launcher.jar --nogui