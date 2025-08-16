#!/usr/bin/env sh
set -e

# remove files inside the world directory, but not the world directory itself!
rm -rf world/*

sed -i.bak \
  -e 's/^elapsedTicks = .*/elapsedTicks = 0/' \
  -e 's/^currentPlayer = .*/currentPlayer = 0/' \
  -e 's/^totalTicks = .*/totalTicks = 0/' \
  config/player-switch/config.toml

echo "Run was reset"