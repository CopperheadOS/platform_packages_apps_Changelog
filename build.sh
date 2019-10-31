#!/bin/bash

dir=$(dirname "$0")

build_dir="$1"

if [ -z "$build_dir" ]; then
    build_dir="."
fi
build_dir+="/.gradle"

if [ ! -d "$build_dir" ]; then
    mkdir -p "$build_dir"
fi

versionCode=$(date +%s)
versionName=$(get_build_var BUILD_NUMBER)

export GRADLE_USER_HOME=$build_dir

"$dir"/gradlew -p "$dir" clean assembleRelease -PversionCode="$versionCode" -PversionName="$versionName"