#!/usr/bin/env bash
# Builds RadarNebo-<version>-x86_64.AppImage from the Compose distributable, which bundles its own trimmed JRE.
#
#   ./gradlew :app:desktop:createDistributable -PappVersion=1.2.3
#   packaging/linux/appimage/build.sh 1.2.3 [output-dir]
#
# The output directory defaults to build/linux. appimagetool and the AppImage runtime are downloaded at pinned
# versions and checked against their SHA-256 sums.
set -euo pipefail

version=${1:?usage: build.sh <version> [output-dir]}
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
out=$(mkdir -p "${2:-$repo/build/linux}" && cd "${2:-$repo/build/linux}" && pwd)
dist=$repo/app/desktop/build/compose/binaries/main/app/RadarNebo
linux=$repo/packaging/linux
id=com.gusak.lab.radarnebo

appimagetool_url=https://github.com/AppImage/appimagetool/releases/download/1.9.1/appimagetool-x86_64.AppImage
appimagetool_sha256=ed4ce84f0d9caff66f50bcca6ff6f35aae54ce8135408b3fa33abfc3cb384eb0
runtime_url=https://github.com/AppImage/type2-runtime/releases/download/20251108/runtime-x86_64
runtime_sha256=2fca8b443c92510f1483a883f60061ad09b46b978b2631c807cd873a47ec260d

[[ -x $dist/bin/RadarNebo ]] || { echo "No distributable at $dist: run ./gradlew :app:desktop:createDistributable" >&2; exit 1; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

fetch() { # url sha256 file
    curl -fsSL --retry 3 -o "$3" "$1"
    echo "$2  $3" | sha256sum --check --quiet
}
fetch "$appimagetool_url" "$appimagetool_sha256" "$work/appimagetool"
fetch "$runtime_url" "$runtime_sha256" "$work/runtime"
chmod +x "$work/appimagetool"

appdir=$work/RadarNebo.AppDir
mkdir -p "$appdir/usr/lib"
cp -a "$dist" "$appdir/usr/lib/radarnebo"
install -Dm644 "$linux/$id.desktop" -t "$appdir/usr/share/applications"
install -Dm644 "$linux/$id.metainfo.xml" -t "$appdir/usr/share/metainfo"
install -Dm644 "$repo/app/desktop/icons/radarnebo.png" "$appdir/usr/share/icons/hicolor/512x512/apps/$id.png"
# appimagetool takes the desktop entry and icon from the AppDir's top level.
ln -s "usr/share/applications/$id.desktop" "$appdir/$id.desktop"
ln -s "usr/share/icons/hicolor/512x512/apps/$id.png" "$appdir/$id.png"
ln -s "$id.png" "$appdir/.DirIcon"
# The launcher finds its JRE from its real path, so AppRun can be a link to it.
ln -s usr/lib/radarnebo/bin/RadarNebo "$appdir/AppRun"

# APPIMAGE_EXTRACT_AND_RUN: run appimagetool without FUSE, which CI runners and containers lack.
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$work/appimagetool" --no-appstream --runtime-file "$work/runtime" \
    "$appdir" "$out/RadarNebo-$version-x86_64.AppImage"
echo "$out/RadarNebo-$version-x86_64.AppImage"
