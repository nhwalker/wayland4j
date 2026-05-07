# References

Canonical sources for the Wayland protocol and tooling. We aim to match the
upstream behavior of `wayland-scanner` and the wire protocol exactly, so when
in doubt these are the authorities.

## Official Site & Docs

- **Wayland project home** — https://wayland.freedesktop.org/
- **Wayland documentation (HTML)** — https://wayland.freedesktop.org/docs/html/
  - Includes the wire format chapter, type system, and protocol overview.

## Core Repositories

- **libwayland (C reference implementation + `wayland-scanner`)**
  https://gitlab.freedesktop.org/wayland/wayland
  - Core protocol XML: `protocol/wayland.xml`
  - Protocol DTD: `protocol/wayland.dtd`
  - Scanner source we are emulating in Java (annotation processor parity target):
    `src/scanner.c`
  - C wire helpers: `src/connection.c`, `src/wayland-private.h`
- **wayland-protocols (stable, staging, and unstable extensions)**
  https://gitlab.freedesktop.org/wayland/wayland-protocols
  - XML files live under `stable/`, `staging/`, `unstable/`.
  - `README.md` explains the stability tiers and naming rules.
- **Weston (reference compositor — useful for cross-checking semantics)**
  https://gitlab.freedesktop.org/wayland/weston

## Learning Material

- **The Wayland Book (Drew DeVault)** — https://wayland-book.com/
  - Wire protocol chapter: https://wayland-book.com/protocol-design/wire-protocol.html
  - Best ground-up explanation of the object model, registry, and message
    framing. Read this before designing the codec layer.

## What to Study for the Annotation Processor

When implementing the code generator, mirror these aspects from
`wayland-scanner`:

1. **XML schema** — `protocol/wayland.dtd` defines `<protocol>`, `<interface>`,
   `<request>`, `<event>`, `<enum>`, `<arg>` and the legal attribute set.
2. **Argument types** — `int`, `uint`, `fixed`, `string`, `object`, `new_id`,
   `array`, `fd`. See the wire-format chapter for sizing and alignment rules.
3. **Versioning** — `since` and `deprecated-since` on requests/events/enums,
   and per-interface `version` attributes.
4. **Enums and bitfields** — `<enum bitfield="true">` plus `<entry>` semantics.
5. **`new_id` without an interface** — the special three-arg encoding
   (interface name string, version uint, new_id uint) used by `wl_registry.bind`.
6. **Destructor semantics** — `type="destructor"` on requests.
7. **Description / summary tags** — for generated Javadoc.

## Conventions To Follow

- Keep generated Java identifiers consistent with upstream C naming where
  practical (e.g. `wl_surface` → `WlSurface`, `wl_surface.attach` →
  `WlSurface.attach`). Document any deviations explicitly.
- Treat `wayland.xml` from upstream as the source of truth — do not hand-edit
  vendored copies; refresh them from the pinned commit.

## Vendored XML Pin

`wayland-protocol/src/main/resources/protocol/wayland.xml` is vendored from
upstream tag **1.25.0**, commit
[`3e673a438b0a9749e3bdf5cac4befac86333024c`](https://gitlab.freedesktop.org/wayland/wayland/-/commit/3e673a438b0a9749e3bdf5cac4befac86333024c).
To refresh:

```sh
curl -fsS \
  "https://gitlab.freedesktop.org/wayland/wayland/-/raw/<NEW_TAG>/protocol/wayland.xml" \
  -o wayland-protocol/src/main/resources/protocol/wayland.xml
```

Then update this section with the new tag and commit hash.
