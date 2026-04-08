#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"
SRC_DIR="$ROOT_DIR/src/main/java"
CLASSPATH="$OUT_DIR:$ROOT_DIR/libs/jSerialComm-2.9.1.jar:$ROOT_DIR/libs/lib_connect.jar:$ROOT_DIR/libs/lib_reader.jar"

mkdir -p "$OUT_DIR"
javac -cp "$ROOT_DIR/libs/jSerialComm-2.9.1.jar:$ROOT_DIR/libs/lib_connect.jar:$ROOT_DIR/libs/lib_reader.jar" \
  -d "$OUT_DIR" \
  $(find "$SRC_DIR" -name "*.java")

exec java -cp "$CLASSPATH" it.icron.rfid.UhfReaderApp "$@"
