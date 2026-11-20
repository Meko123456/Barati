#!/usr/bin/env python3
"""Derives the dark and tinted app icons from the light one, for iOS 18 Home Screens.

A Home Screen can be dark or tinted now, and an app that ships only its light icon gets the
system's own guess at the other two. There is no vector source for this icon, so the glyph is
lifted off the light PNG by colour, then laid on the same background dimmed to a third of its
brightness for the dark icon, and written alone in grayscale on transparency for the tinted one,
which the system colours with the user's tint.

    python3 iosApp/Icon/derive_appearances.py

Needs Pillow and NumPy. Run it again whenever icon-1024.png changes.
"""

import pathlib

import numpy as np
from PIL import Image, ImageEnhance

ICONSET = pathlib.Path(__file__).resolve().parents[1] / "Sources/Assets.xcassets/AppIcon.appiconset"
LIGHT = ICONSET / "icon-1024.png"
# How much of the light icon's brightness the dark one keeps.
DARK = 0.34

# The glyph is whatever isn't the flat background: the distance from its colour becomes the
# alpha, so the cards and their shadow come out whole and the blue goes.
KEY = (53,71,176)


def glyph_alpha(rgb):
    dist = np.abs(rgb - np.array(KEY)).sum(axis=2)
    return np.clip((dist - 20) * 6, 0, 255)


def main() -> None:
    light = Image.open(LIGHT).convert("RGBA")
    rgb = np.asarray(light)[..., :3].astype(int)
    glyph = Image.fromarray(np.dstack([rgb, glyph_alpha(rgb)]).astype("uint8"))
    dimmed = ImageEnhance.Brightness(light.convert("RGB")).enhance(DARK).convert("RGBA")
    Image.alpha_composite(dimmed, glyph).convert("RGB").save(ICONSET / "icon-1024-dark.png", optimize=True)
    Image.merge("LA", (glyph.convert("L"), glyph.getchannel("A"))).save(ICONSET / "icon-1024-tinted.png", optimize=True)
    for name in ("icon-1024-dark.png", "icon-1024-tinted.png"):
        print(ICONSET / name)


if __name__ == "__main__":
    main()
