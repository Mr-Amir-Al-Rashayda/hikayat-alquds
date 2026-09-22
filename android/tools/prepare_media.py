"""
Prepares the binary assets the Android app ships with.

Three jobs, all deterministic and repeatable from the web app's `public/`
folder so the two clients never drift:

  1. Photographs  - re-encoded for a phone screen. The originals are up to 6 MB
                    each, which is archive quality, not app quality.
  2. Fonts        - the Thmanyah family is published as .woff2 for the web;
                    Android's font loader needs TrueType, so each face is
                    converted once here.
  3. Launcher icon- an adaptive icon (background + foreground + monochrome)
                    generated from the official logo, plus legacy round/square
                    bitmaps for API levels below 26.

Run:  python android/tools/prepare_media.py
"""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[2]
WEB = ROOT / "frontend" / "public"
APP = ROOT / "android" / "app" / "src" / "main"
ASSETS = APP / "assets"
RES = APP / "res"

# A phone never shows more than ~1200 px across; 1400 leaves room for zooming
# into a gallery image without carrying six megabytes per photograph.
MAX_EDGE = 1400
JPEG_QUALITY = 82

BRAND_BG = (245, 245, 240)     # --color-brand-bg
BRAND_OLIVE = (90, 90, 64)     # --color-brand-olive


def human(size: int) -> str:
    return f"{size / 1_048_576:.1f} MB" if size > 1_048_576 else f"{size / 1024:.0f} KB"


# --- 1. photographs ---------------------------------------------------------
def convert_images() -> None:
    source = WEB / "images" / "jerusalem"
    target = ASSETS / "images" / "jerusalem"
    if target.exists():
        shutil.rmtree(target)
    before = after = 0
    count = 0
    for path in sorted(source.rglob("*")):
        if path.suffix.lower() not in {".jpg", ".jpeg", ".png"}:
            continue
        relative = path.relative_to(source)
        destination = target / relative.with_suffix(".jpg")
        destination.parent.mkdir(parents=True, exist_ok=True)

        with Image.open(path) as image:
            image = ImageOps.exif_transpose(image).convert("RGB")
            image.thumbnail((MAX_EDGE, MAX_EDGE), Image.Resampling.LANCZOS)
            image.save(destination, "JPEG", quality=JPEG_QUALITY, optimize=True, progressive=True)

        before += path.stat().st_size
        after += destination.stat().st_size
        count += 1
    print(f"  photographs : {count} files, {human(before)} -> {human(after)}")


# --- 2. fonts ---------------------------------------------------------------
FONT_FACES = {
    "thmanyah-sans-regular": "thmanyah_sans_regular",
    "thmanyah-sans-medium": "thmanyah_sans_medium",
    "thmanyah-sans-bold": "thmanyah_sans_bold",
    "thmanyah-serif-text-regular": "thmanyah_serif_text_regular",
    "thmanyah-serif-text-bold": "thmanyah_serif_text_bold",
    "thmanyah-serif-display-regular": "thmanyah_serif_display_regular",
    "thmanyah-serif-display-bold": "thmanyah_serif_display_bold",
    "thmanyah-serif-display-black": "thmanyah_serif_display_black",
}


def convert_fonts() -> None:
    from fontTools.ttLib import TTFont

    target = RES / "font"
    target.mkdir(parents=True, exist_ok=True)
    for web_name, android_name in FONT_FACES.items():
        source = WEB / "fonts" / f"{web_name}.woff2"
        if not source.exists():
            print(f"  ! missing font {source.name}", file=sys.stderr)
            continue
        font = TTFont(str(source))
        font.flavor = None          # drop woff2 compression -> plain TrueType
        font.save(str(target / f"{android_name}.ttf"))
    total = sum(p.stat().st_size for p in target.glob("*.ttf"))
    print(f"  fonts       : {len(list(target.glob('*.ttf')))} faces, {human(total)}")


# --- 3. launcher icon -------------------------------------------------------
def _logo_with_alpha() -> Image.Image:
    """The logo is published on a white card; lift it off for the icon layer."""
    with Image.open(WEB / "logo-icon.png") as raw:
        logo = raw.convert("RGBA")
    pixels = logo.load()
    width, height = logo.size
    for y in range(height):
        for x in range(width):
            r, g, b, _ = pixels[x, y]
            if r > 243 and g > 243 and b > 240:
                pixels[x, y] = (r, g, b, 0)
    return logo.crop(logo.getbbox())


def _mipmap(name: str, image: Image.Image, sizes: dict[str, int]) -> None:
    for density, size in sizes.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(folder / f"{name}.png", "PNG", optimize=True)


def build_icons() -> None:
    logo = _logo_with_alpha()

    # Adaptive foreground: the icon lives inside the middle 66% safe zone, so
    # the logo is drawn at 60% of the 432 px layer and the launcher can mask,
    # rotate and parallax it without clipping the walls.
    layer = 432
    foreground = Image.new("RGBA", (layer, layer), (0, 0, 0, 0))
    fitted = ImageOps.contain(logo, (int(layer * 0.60), int(layer * 0.60)), Image.Resampling.LANCZOS)
    foreground.paste(fitted, ((layer - fitted.width) // 2, (layer - fitted.height) // 2), fitted)
    # drawable-nodpi, not drawable: these are density-independent icon layers
    # and Android would otherwise rescale them as if they were mdpi.
    (RES / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
    foreground.save(RES / "drawable-nodpi" / "ic_launcher_foreground.png", "PNG", optimize=True)

    # Themed (Android 13+) icons are tinted by the system, so the monochrome
    # layer must carry shape in the alpha channel only.
    monochrome = Image.new("RGBA", (layer, layer), (0, 0, 0, 0))
    silhouette = Image.new("RGBA", fitted.size, (0, 0, 0, 255))
    monochrome.paste(silhouette, ((layer - fitted.width) // 2, (layer - fitted.height) // 2), fitted)
    monochrome.save(RES / "drawable-nodpi" / "ic_launcher_monochrome.png", "PNG", optimize=True)

    # Legacy launchers (API 24-25) get baked bitmaps.
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    square = Image.new("RGBA", (512, 512), BRAND_BG + (255,))
    inner = ImageOps.contain(logo, (410, 410), Image.Resampling.LANCZOS)
    square.paste(inner, ((512 - inner.width) // 2, (512 - inner.height) // 2), inner)
    _mipmap("ic_launcher", square, densities)

    round_icon = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    mask = Image.new("L", (512, 512), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, 511, 511), fill=255)
    round_icon.paste(square, (0, 0), mask)
    _mipmap("ic_launcher_round", round_icon, densities)

    # Play Store listing icon.
    (ROOT / "android" / "store").mkdir(parents=True, exist_ok=True)
    square.convert("RGB").save(ROOT / "android" / "store" / "play-store-icon.png", "PNG", optimize=True)

    # In-app wordmarks, sized for a phone header rather than a print sheet.
    (ASSETS / "brand").mkdir(parents=True, exist_ok=True)
    for name, width in (("logo-horizontal", 900), ("logo-vertical", 700), ("logo-icon", 512)):
        with Image.open(WEB / f"{name}.png") as raw:
            wordmark = raw.convert("RGB")
            wordmark.thumbnail((width, width), Image.Resampling.LANCZOS)
            wordmark.save(ASSETS / "brand" / f"{name}.jpg", "JPEG", quality=88, optimize=True)
    print("  icons       : adaptive + monochrome + legacy mipmaps + store icon")


if __name__ == "__main__":
    print("Preparing Hikayat AlQuds Android media")
    convert_images()
    convert_fonts()
    build_icons()
    total = sum(p.stat().st_size for p in ASSETS.rglob("*") if p.is_file())
    print(f"\nBundled assets: {human(total)}")
