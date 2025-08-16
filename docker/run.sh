#!/usr/bin/env sh

set -e

# copy files to /data mount if needed
rsync -r --ignore-existing /template/* .

if [ "$EULA" = "true" ]; then
    echo "eula=true" > eula.txt
fi

: "${MAX_MEMORY:=2G}"

echo "$MAX_MEMORY"
java "-Xmx$MAX_MEMORY" -jar fabric-server-launcher.jar --nogui