# Vendored wayland-protocols sources

The XML files under `stable/` are copied verbatim from upstream:

- Repository: https://gitlab.freedesktop.org/wayland/wayland-protocols
- Tag: `1.48`

Vendored protocols:

- `stable/xdg-shell/xdg-shell.xml`
- `stable/viewporter/viewporter.xml`
- `stable/presentation-time/presentation-time.xml`
- `stable/linux-dmabuf/linux-dmabuf-v1.xml`
- `stable/tablet/tablet-v2.xml`

## Refresh procedure

```sh
TAG=1.48
DEST=wayland-protocol/src/main/resources/org/wayland4j/protocol/wayland-protocols/stable
BASE="https://gitlab.freedesktop.org/wayland/wayland-protocols/-/raw/${TAG}/stable"
curl -sSfL "${BASE}/xdg-shell/xdg-shell.xml"             -o "${DEST}/xdg-shell/xdg-shell.xml"
curl -sSfL "${BASE}/viewporter/viewporter.xml"           -o "${DEST}/viewporter/viewporter.xml"
curl -sSfL "${BASE}/presentation-time/presentation-time.xml" -o "${DEST}/presentation-time/presentation-time.xml"
curl -sSfL "${BASE}/linux-dmabuf/linux-dmabuf-v1.xml"    -o "${DEST}/linux-dmabuf/linux-dmabuf-v1.xml"
curl -sSfL "${BASE}/tablet/tablet-v2.xml"                -o "${DEST}/tablet/tablet-v2.xml"
```

Update the `TAG` value above and re-run the build to regenerate Java sources.
