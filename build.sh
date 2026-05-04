#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src/main/java"
RES_DIR="$SCRIPT_DIR/src/main/resources"
BUILD_DIR="$SCRIPT_DIR/build"
LIB_DIR="$SCRIPT_DIR/lib"

MOD_ID="diy_the_spire"

echo "=== Building DIY_the_spire ==="

mkdir -p "$BUILD_DIR"

if [ -d "$LIB_DIR" ]; then
    CLASSPATH="$LIB_DIR/*"
else
    echo "Warning: lib directory not found"
    CLASSPATH=""
fi

echo "Compiling Java sources..."
javac -source 1.8 -target 1.8 \
    -cp "$CLASSPATH" \
    -d "$BUILD_DIR" \
    $(find "$SRC_DIR" -name "*.java")

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Copying resources..."
cp -r "$RES_DIR"/* "$BUILD_DIR"

echo "Creating JAR..."
jar -cvf "$SCRIPT_DIR/$MOD_ID.jar" -C "$BUILD_DIR" .

if [ $? -eq 0 ]; then
    echo "Build successful: $MOD_ID.jar"
    rm -rf "$BUILD_DIR"
else
    echo "JAR creation failed!"
    exit 1
fi