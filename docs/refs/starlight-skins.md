# Starlight Skins API — contract snapshot

**Status:** De-facto contract. Starlight Skins is a free public API with no published
OpenAPI spec; this doc captures what we observed from the working LumaSG integration
and should be revisited if the upstream behaviour changes.

**Upstream host:** `starlightskins.lunareclipse.studio`
**Used by:** REQ-009 (winner chat celebration with pixel-art head)
**Reference implementation:** `D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt`
lines 60-180 (`renderPixelArtHead` function).

---

## 1. URL pattern

The endpoint is templated in `config.yml` under `celebration.pixel-art.api-url`:

```
https://starlightskins.lunareclipse.studio/render/<type>/<uuid>/<part>
```

Default pinned by `config.yml`:

```
https://starlightskins.lunareclipse.studio/render/default/<uuid>/face
```

### Substitutions

The adapter performs literal string replacement on the configured URL before
issuing the request (matches LumaSG `Celebration.kt:72-74`):

| Token    | Replaced with                                  |
|----------|------------------------------------------------|
| `<uuid>` | `winnerUuid.toString()` (hyphenated form)      |
| `<name>` | Winner's display name (kept for compatibility) |

Only `<uuid>` is needed for the face render. `<name>` is supported for parity
with the LumaSG config schema so operators can swap render types without code
changes.

---

## 2. Render types & parts

The path segment after `/render/` selects a render *type* (camera/pose) and the
segment after `<uuid>/` selects the *part* of the skin to crop. We exclusively
use:

| Type      | Part   | Purpose                                                          |
|-----------|--------|------------------------------------------------------------------|
| `default` | `face` | Flat 8x8 head crop, ideal for low-resolution pixel-art rendering |

Other render types exist upstream (e.g. `default` full body, `marching`,
isometric variants). We do not enumerate them here — if a future feature needs
one, add a row above and update `config.yml` rather than hard-coding the URL.

---

## 3. Response format

* **Content-Type:** `image/png` (binary PNG).
* **Decoding:** `javax.imageio.ImageIO.read(InputStream)` against the
  connection's input stream. Returns a `BufferedImage` or `null` on a malformed
  response.
* **Native dimensions:** The `face` render is nominally 8x8 but the API may
  return a higher-resolution image. The adapter MUST scale to the configured
  `celebration.pixel-art.size` (default `8`) before pixel iteration. See LumaSG
  `Celebration.kt:89-97` for the `Graphics2D.drawImage` scaling block.

---

## 4. Timeout discipline

LumaSG sets both timeouts on the `URLConnection` to **5 seconds** and treats
*any* failure (network, decode, null image) as a silent skip — celebration text
still posts, just without the pixel art. Our adapter MUST match this:

```kotlin
val conn = url.openConnection().apply {
    connectTimeout = 5_000      // 5s — LumaSG Celebration.kt:84
    readTimeout    = 5_000      // 5s — LumaSG Celebration.kt:85
    setRequestProperty("User-Agent", "EnthusiaGiveaway-Plugin")
}
```

Rationale: this is a celebration cosmetic, not a critical path. Blocking
winner announcement on a flaky third-party API would degrade UX worse than
omitting the art. Log at `warn` and return `null`; never propagate.

The fetch MUST run off the main thread (LumaSG uses `Dispatchers.IO`).

---

## 5. User-Agent

| Plugin            | User-Agent string         |
|-------------------|---------------------------|
| LumaSG (upstream) | `LumaSG-Plugin`           |
| EnthusiaGiveaway  | `EnthusiaGiveaway-Plugin` |

Identifying ourselves is courteous and helps the upstream operator distinguish
traffic if rate-limiting decisions become necessary.

---

## 6. Pixel-art rendering

Once the `BufferedImage` is in hand, each pixel is converted to a coloured
Adventure `Component`:

* Iterate `x in 0 until size`, `y in 0 until size`.
* Read `image.getRGB(x, y)`; split into ARGB bytes.
* If `alpha < 128`, render the pixel as `NamedTextColor.BLACK` (transparent
  background sentinel — see LumaSG `Celebration.kt:122`).
* Otherwise, colour the pixel with
  `net.kyori.adventure.text.format.TextColor.color(r, g, b)`.
* The glyph itself is the configurable Unicode square from
  `celebration.pixel-art.character` (default `⬛`, U+2B1B).

One row of the image becomes one chat line; rows are concatenated into a
single `Component` per row before sending.

---

## 7. Open questions / risks

* **No SLA.** Starlight Skins is community-run. If it goes down or rate-limits
  us, the 5s timeout + silent-skip path is our only defence — keep it.
* **No content-length contract.** The PNG can in principle be arbitrarily
  large. `ImageIO.read` buffers the whole stream; the 5s read timeout caps
  the worst case in practice, but if upstream ever starts returning huge
  renders we should add an explicit size guard.
* **No auth.** Endpoint is unauthenticated; do not add credentials.

---

## 8. References

* `D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt`
  lines 60-180 — canonical reference implementation.
* `config.yml` → `celebration.pixel-art.*` — operator-tunable knobs.
* `docs/requirements.md` → REQ-009 — the requirement this contract serves.
* `docs/tech-stack.md` §4 — pinned-schemas table entry that points here.
