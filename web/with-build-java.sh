#!/usr/bin/env bash
set -euo pipefail

# Cloudflare's build image and the Linux CI runner use x86_64.
archive_url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz'
archive_sha256='bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e'
jdk_dir="$(mktemp -d)"
trap 'rm -rf -- "$jdk_dir"' EXIT
archive="$jdk_dir/temurin.tar.gz"

curl --fail --silent --show-error --location --retry 3 "$archive_url" --output "$archive"
printf '%s  %s\n' "$archive_sha256" "$archive" | sha256sum --check
export JAVA_HOME="$jdk_dir/jdk"
mkdir -- "$JAVA_HOME"
tar -xzf "$archive" -C "$JAVA_HOME" --strip-components=1 --no-same-owner

"$@"
