## Linux desktop app

Three packages of the same app (x86_64). Pick one:

| Package | Install | Updates |
|---|---|---|
| `RadarNebo-*-x86_64.AppImage` | `chmod +x RadarNebo-*-x86_64.AppImage && ./RadarNebo-*-x86_64.AppImage` | Download the new release |
| `RadarNebo-*-x86_64.flatpak` | `flatpak install --user RadarNebo-*-x86_64.flatpak`, then `flatpak run com.gusak.lab.radarnebo` | Install the new bundle the same way |
| `radarnebo_*_amd64.snap` | `sudo snap install --dangerous radarnebo_*_amd64.snap` | Install the new snap the same way |

- The Flatpak runs on the Freedesktop 26.08 runtime from Flathub. If installing can't find it, add Flathub first: `flatpak remote-add --user --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo`.
- The snap isn't from the Snap Store, so it isn't signed: `--dangerous` allows installing it, and it doesn't update by itself.
- Check a download against `SHA256SUMS`: `sha256sum --check --ignore-missing SHA256SUMS`.
