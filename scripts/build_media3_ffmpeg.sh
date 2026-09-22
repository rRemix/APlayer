#!/usr/bin/env bash
set -euo pipefail

# Build AndroidX Media3 1.8.0 FFmpeg audio decoder AAR for APlayer.
#
# Expected layout:
#   APlayer/
#     app/
#     third-party/media3/
#     scripts/build_media3_ffmpeg.sh
#
# Default:
#   Media3: 1.8.0
#   FFmpeg: n6.0
#   NDK:    26.1.10909125 (r26b)
#   API:    21
#   Decoder: flac
#
# Overrides:
#   FFMPEG_DECODERS="flac vorbis opus" ./scripts/build_media3_ffmpeg.sh
#   ANDROID_NDK_HOME=/path/to/ndk ./scripts/build_media3_ffmpeg.sh
#   ANDROID_SDK_ROOT=/path/to/sdk ./scripts/build_media3_ffmpeg.sh
#   NDK_VERSION=26.1.10909125 ./scripts/build_media3_ffmpeg.sh
#   FFMPEG_REF=n6.0 ./scripts/build_media3_ffmpeg.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

MEDIA3_DIR="${ROOT_DIR}/third-party/media3"
FFMPEG_MODULE_PATH="${MEDIA3_DIR}/libraries/decoder_ffmpeg/src/main"
JNI_DIR="${FFMPEG_MODULE_PATH}/jni"
FFMPEG_DIR="${JNI_DIR}/ffmpeg"

OUTPUT_DIR="${ROOT_DIR}/app/libs"
OUTPUT_AAR="${OUTPUT_DIR}/lib-decoder-ffmpeg-release.aar"

MEDIA3_VERSION="${MEDIA3_VERSION:-1.8.0}"
FFMPEG_REF="${FFMPEG_REF:-n6.0}"
FFMPEG_REPO="${FFMPEG_REPO:-https://github.com/FFmpeg/FFmpeg.git}"
NDK_VERSION="${NDK_VERSION:-26.1.10909125}"
CMAKE_VERSION="${CMAKE_VERSION:-3.22.1}"
ANDROID_ABI="${ANDROID_ABI:-21}"
FFMPEG_DECODERS="${FFMPEG_DECODERS:-aac mp3 vorbis opus flac alac ape wmav1 wmav2 wmapro xma1 xma2}"

log() {
  printf '\n\033[1;34m==>\033[0m %s\n' "$*"
}

die() {
  printf '\n\033[1;31mERROR:\033[0m %s\n' "$*" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || die "git not found"
command -v java >/dev/null 2>&1 || die "java not found"
command -v unzip >/dev/null 2>&1 || die "unzip not found"

[[ -d "${MEDIA3_DIR}" ]] || die "Media3 submodule not found: ${MEDIA3_DIR}"
[[ -f "${MEDIA3_DIR}/gradlew" ]] || die "Invalid Media3 checkout: ${MEDIA3_DIR}"
[[ -f "${JNI_DIR}/build_ffmpeg.sh" ]] || die "build_ffmpeg.sh not found under Media3 decoder_ffmpeg"

# Verify the checkout is Media3 1.8.0.
if ! grep -q "releaseVersion = '${MEDIA3_VERSION}'" "${MEDIA3_DIR}/constants.gradle"; then
  CURRENT_VERSION="$(grep -E "releaseVersion = " "${MEDIA3_DIR}/constants.gradle" 2>/dev/null || true)"
  die "Expected Media3 ${MEDIA3_VERSION}, but constants.gradle says: ${CURRENT_VERSION:-unknown}"
fi

detect_android_sdk() {
  local candidate
  for candidate in \
    "${ANDROID_SDK_ROOT:-}" \
    "${ANDROID_HOME:-}" \
    "${HOME}/Library/Android/sdk" \
    "${HOME}/Android/Sdk"; do
    if [[ -n "${candidate}" && -d "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
  return 1
}

ANDROID_SDK="$(detect_android_sdk || true)"
[[ -n "${ANDROID_SDK}" ]] || die "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME."

# Media3 1.8.0 compiles against Android SDK 35.
if [[ ! -d "${ANDROID_SDK}/platforms/android-35" ]]; then
  die "Android SDK platform 35 is missing.
Install it with:
  \"${ANDROID_SDK}/cmdline-tools/latest/bin/sdkmanager\" \"platforms;android-35\""
fi

if [[ ! -d "${ANDROID_SDK}/cmake/${CMAKE_VERSION}" ]]; then
  die "Android SDK CMake ${CMAKE_VERSION} is missing.
Install it with:
  \"${ANDROID_SDK}/cmdline-tools/latest/bin/sdkmanager\" \"cmake;${CMAKE_VERSION}\""
fi

# Media3 1.8.0 FFmpeg instructions were tested with NDK r26b.
if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
  NDK_PATH="${ANDROID_NDK_HOME}"
else
  NDK_PATH="${ANDROID_SDK}/ndk/${NDK_VERSION}"
fi

if [[ ! -d "${NDK_PATH}" ]]; then
  die "Android NDK ${NDK_VERSION} not found at:
  ${NDK_PATH}

Install it with:
  \"${ANDROID_SDK}/cmdline-tools/latest/bin/sdkmanager\" \"ndk;${NDK_VERSION}\""
fi

# Find the NDK host toolchain directory rather than assuming Intel/Apple Silicon.
if [[ -d "${NDK_PATH}/toolchains/llvm/prebuilt/darwin-x86_64" ]]; then
  HOST_PLATFORM="darwin-x86_64"
elif [[ -d "${NDK_PATH}/toolchains/llvm/prebuilt/linux-x86_64" ]]; then
  HOST_PLATFORM="linux-x86_64"
else
  HOST_PLATFORM="$(find "${NDK_PATH}/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null | head -n 1 | xargs -n 1 basename || true)"
  [[ -n "${HOST_PLATFORM}" ]] || die "Unable to determine NDK host platform under ${NDK_PATH}"
fi

# Media3 Gradle build needs an SDK location when built as a standalone checkout.
printf 'sdk.dir=%s\n' "${ANDROID_SDK}" > "${MEDIA3_DIR}/local.properties"

# Bash 3.2 compatible array creation (important for stock macOS bash).
read -r -a ENABLED_DECODERS <<< "${FFMPEG_DECODERS}"
[[ "${#ENABLED_DECODERS[@]}" -gt 0 ]] || die "FFMPEG_DECODERS is empty"

log "Configuration"
printf 'APlayer root : %s\n' "${ROOT_DIR}"
printf 'Media3       : %s\n' "${MEDIA3_VERSION}"
printf 'Media3 path  : %s\n' "${MEDIA3_DIR}"
printf 'FFmpeg ref   : %s\n' "${FFMPEG_REF}"
printf 'Android SDK  : %s\n' "${ANDROID_SDK}"
printf 'NDK          : %s\n' "${NDK_PATH}"
printf 'CMake        : %s\n' "${CMAKE_VERSION}"
printf 'Host         : %s\n' "${HOST_PLATFORM}"
printf 'Android API  : %s\n' "${ANDROID_ABI}"
printf 'Decoders     : %s\n' "${ENABLED_DECODERS[*]}"

log "Preparing FFmpeg ${FFMPEG_REF}"
if [[ ! -d "${FFMPEG_DIR}/.git" ]]; then
  rm -rf "${FFMPEG_DIR}"
  git clone --depth 1 --branch "${FFMPEG_REF}" "${FFMPEG_REPO}" "${FFMPEG_DIR}"
else
  if [[ -n "$(git -C "${FFMPEG_DIR}" status --porcelain)" ]]; then
    die "FFmpeg source tree has local changes: ${FFMPEG_DIR}
Commit/stash/remove them before rebuilding."
  fi

  git -C "${FFMPEG_DIR}" fetch --depth 1 origin "refs/tags/${FFMPEG_REF}:refs/tags/${FFMPEG_REF}" 2>/dev/null \
    || git -C "${FFMPEG_DIR}" fetch --depth 1 origin "${FFMPEG_REF}"
  git -C "${FFMPEG_DIR}" checkout --detach "${FFMPEG_REF}"
fi

# Avoid stale libraries if the decoder list changes between builds.
rm -rf "${FFMPEG_DIR}/android-libs"
(
  cd "${FFMPEG_DIR}"
  make distclean >/dev/null 2>&1 || true
)

log "Building FFmpeg static libraries"
(
  cd "${JNI_DIR}"
  ./build_ffmpeg.sh \
    "${FFMPEG_MODULE_PATH}" \
    "${NDK_PATH}" \
    "${HOST_PLATFORM}" \
    "${ANDROID_ABI}" \
    "${ENABLED_DECODERS[@]}"
)

log "Building Media3 decoder_ffmpeg AAR"
(
  cd "${MEDIA3_DIR}"
  ./gradlew :lib-decoder-ffmpeg:assembleRelease
)

AAR_PATH="$(
  find "${MEDIA3_DIR}/libraries/decoder_ffmpeg" \
    -type f \
    -path '*/outputs/aar/*.aar' \
    -name '*release*.aar' \
    -print \
    | head -n 1
)"

[[ -n "${AAR_PATH}" && -f "${AAR_PATH}" ]] || die "Gradle finished, but release AAR was not found."

mkdir -p "${OUTPUT_DIR}"
cp -f "${AAR_PATH}" "${OUTPUT_AAR}"

log "Verifying AAR"
JNI_COUNT="$(unzip -l "${OUTPUT_AAR}" | grep -c 'libffmpegJNI\.so' || true)"
[[ "${JNI_COUNT}" -gt 0 ]] || die "AAR does not contain libffmpegJNI.so"

if command -v shasum >/dev/null 2>&1; then
  SHA256="$(shasum -a 256 "${OUTPUT_AAR}" | awk '{print $1}')"
elif command -v sha256sum >/dev/null 2>&1; then
  SHA256="$(sha256sum "${OUTPUT_AAR}" | awk '{print $1}')"
else
  SHA256="(sha256 tool not found)"
fi

AAR_SIZE="$(du -h "${OUTPUT_AAR}" | awk '{print $1}')"

log "Done"
printf 'AAR      : %s\n' "${OUTPUT_AAR}"
printf 'Size     : %s\n' "${AAR_SIZE}"
printf 'SHA-256  : %s\n' "${SHA256}"
printf 'JNI libs : %s\n' "${JNI_COUNT}"
printf 'Decoders : %s\n' "${ENABLED_DECODERS[*]}"
