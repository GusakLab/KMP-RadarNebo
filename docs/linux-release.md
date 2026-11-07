# Linux release of the desktop app

The desktop client (`app/desktop`) ships on Linux as an AppImage, a Flatpak bundle and a Snap, all attached to a
GitHub release. All three wrap the same Compose distributable, which bundles its own trimmed JRE, so none of them
needs Java on the user's machine.

| File | Built from | Settings live in |
|---|---|---|
| `RadarNebo-<version>-x86_64.AppImage` | [`packaging/linux/appimage/build.sh`](../packaging/linux/appimage/build.sh) | `~/.radarnebo/` |
| `RadarNebo-<version>-x86_64.flatpak` | [`packaging/linux/flatpak/com.gusak.lab.radarnebo.yml`](../packaging/linux/flatpak/com.gusak.lab.radarnebo.yml) | `~/.var/app/com.gusak.lab.radarnebo/.radarnebo/` |
| `radarnebo_<version>_amd64.snap` | [`snap/snapcraft.yaml`](../snap/snapcraft.yaml) | `~/snap/radarnebo/current/.radarnebo/` |

The desktop entry and AppStream metadata that all three install are in [`packaging/linux`](../packaging/linux),
under the app ID `com.gusak.lab.radarnebo`.

## Making a release

Push a tag `v<major>.<minor>.<patch>`:

```sh
git tag v1.2.3
git push origin v1.2.3
```

[`release-linux.yml`](../.github/workflows/release-linux.yml) then:

1. Runs `:app:desktop:test` and builds the distributable with version `1.2.3` (`-PappVersion=1.2.3`).
2. Builds the AppImage, the Flatpak (in the Freedesktop 26.08 builder image) and the Snap (`core24`, on an
   Ubuntu 24.04 runner) in parallel.
3. Creates the release `v1.2.3` with the three files, a `SHA256SUMS` file, install instructions
   ([`packaging/linux/release-notes.md`](../packaging/linux/release-notes.md)) and generated notes. Rerunning it for
   the same tag replaces the files of that release.

A pull request that changes the packaging, or a manual run from the Actions tab, builds the same three files as
workflow artifacts without releasing anything. Those builds get version `0.0.<run number>`.

### Server address and key

The workflow builds without a `client.properties`, so the packages use `https://radar-nebo.worxbend.com` with no
key. The app has no setting for the server or the key, so the packages reach that server only while it accepts
clients without one of its `CLIENT_KEYS`. Baking a key in, as a local `client.properties` does, would publish it to
anyone who downloads a package.

## Building locally

All three start from the distributable:

```sh
./gradlew :app:desktop:createDistributable -PappVersion=1.2.3
```

- **AppImage:** `packaging/linux/appimage/build.sh 1.2.3` writes `build/linux/RadarNebo-1.2.3-x86_64.AppImage`. It
  downloads appimagetool and the AppImage runtime at pinned versions and checks their checksums.
- **Flatpak:** with `flatpak-builder` (or `flatpak run org.flatpak.Builder`) and the Freedesktop 26.08 SDK from
  Flathub:

  ```sh
  flatpak-builder --user --install-deps-from=flathub --repo=build/flatpak-repo --force-clean \
    build/flatpak packaging/linux/flatpak/com.gusak.lab.radarnebo.yml
  flatpak build-bundle --runtime-repo=https://dl.flathub.org/repo/flathub.flatpakrepo \
    build/flatpak-repo build/linux/RadarNebo.flatpak com.gusak.lab.radarnebo
  ```

- **Snap:** `snapcraft pack` in the repository root, which builds in an LXD container (or Multipass), and
  `sudo snap install --dangerous radarnebo_*.snap` to try it. The version stays `0.0.0`; the release workflow
  sets it.

## Not covered

- Only x86_64. An arm64 build would need the distributable built on an arm64 runner, since the bundled JRE and the
  Skia libraries are per architecture.
- The packages go to GitHub releases only, not to Flathub or the Snap Store, so they don't update by themselves.
  Publishing the snap to the Store would need the name `radarnebo` registered and a `SNAPCRAFT_STORE_CREDENTIALS`
  secret.
- `project_license` in the AppStream metadata and `license` in `snapcraft.yaml` are `MIT`, as in
  [LICENSE](../LICENSE). Change all three together.
