# Vendored Wayland protocol source

`wayland.xml` and `wayland.dtd` in this directory are copied verbatim from
upstream:

- Repository: https://gitlab.freedesktop.org/wayland/wayland
- Tag: `1.23.1`
- Files: `protocol/wayland.xml`, `protocol/wayland.dtd`

## Refresh procedure

```sh
TAG=1.23.1
DEST=wayland-protocol/src/main/resources/org/wayland4j/protocol
curl -sSfL "https://gitlab.freedesktop.org/wayland/wayland/-/raw/${TAG}/protocol/wayland.xml" -o "${DEST}/wayland.xml"
curl -sSfL "https://gitlab.freedesktop.org/wayland/wayland/-/raw/${TAG}/protocol/wayland.dtd" -o "${DEST}/wayland.dtd"
```

Update the `TAG` value above and re-run the build to regenerate Java sources.
