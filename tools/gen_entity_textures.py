"""The beasts' skins, painted over Minecraft's own animals (hoglin, wolf, ocelot, polar bear,
cow), whose UV layouts the renderers reuse. Each pixel's brightness in the vanilla skin picks a
colour along a palette of several stops (shadow, base, highlight, pale belly), then fur is laid
over it: per-pixel grain, darker streaks running with the hair, light-tipped guard hairs, and
for the cats, irregular tapering stripes or rosettes rather than regular bands.

Usage: gen_entity_textures.py <vanilla textures/entity dir>"""
import math
import os
import random
import sys
from PIL import Image

SRC = sys.argv[1]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "realmlord", "textures", "entity")


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(stops, t):
    """A colour along a palette of stops, evenly spaced from dark (t=0) to light (t=1)."""
    t = max(0.0, min(1.0, t))
    n = len(stops) - 1
    i = min(n - 1, int(t * n))
    return lerp(hexc(stops[i]), hexc(stops[i + 1]), t * n - i)


def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def noise(x, y, seed):
    """Smooth-ish value noise in 0..1 (two octaves), so markings wander rather than repeat."""
    def v(ix, iy):
        return random.Random((ix * 73856093) ^ (iy * 19349663) ^ seed).random()

    def smooth(fx, fy, scale):
        x0, y0 = int(fx // scale), int(fy // scale)
        tx, ty = fx / scale - x0, fy / scale - y0
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = v(x0, y0) + (v(x0 + 1, y0) - v(x0, y0)) * tx
        b = v(x0, y0 + 1) + (v(x0 + 1, y0 + 1) - v(x0, y0 + 1)) * tx
        return a + (b - a) * ty

    return 0.65 * smooth(x, y, 6) + 0.35 * smooth(x, y, 3)


def paint(src, stops, gamma=1.0, keep_light=None, seed=1, grain=0.07, streak=0.12, tips=None, marks=None, mark_col="16120e",
          spots=None, spot_col=None):
    """Repaints a vanilla skin.

    keep_light: pixels at least this bright (eyes, tusks, horns) stay as they were.
    tips: colour of light-tipped guard hairs (grizzle), sprinkled over the darker fur.
    marks: "stripes" (a tiger's), "rosettes" (a leopard's) or None.
    spots: pixels darker than this in the source (an ocelot's spots) take spot_col (or the base).
    """
    img = Image.open(os.path.join(SRC, src)).convert("RGBA")
    px = img.load()
    rnd = random.Random(seed)
    lo, hi = 255, 0
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                l = 0.3 * r + 0.59 * g + 0.11 * b
                lo, hi = min(lo, l), max(hi, l)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if not a: continue
            l = 0.3 * r + 0.59 * g + 0.11 * b
            if keep_light and l >= keep_light: continue
            t = ((l - lo) / max(1, hi - lo)) ** gamma
            if spots and l < spots and spot_col is None:
                t = min(1.0, t + 0.35)     # the ocelot's own spots fade into the ground colour
            c = ramp(stops, t)
            # Fur: grain, streaks running with the hair (down the texture), light guard hairs.
            f = 1.0 + rnd.uniform(-grain, grain)
            if random.Random(x * 131 + (y // 2) * 7 + seed).random() < 0.18: f -= streak
            c = mul(c, f)
            if tips and t < 0.75 and rnd.random() < 0.05: c = lerp(c, hexc(tips), 0.4)
            if marks == "stripes" and t < 0.92:
                # Irregular, tapering stripes: a wavy band through value noise.
                w = math.sin(x * 0.95 + 3.0 * noise(x, y, seed) + y * 0.18)
                if w > 0.72 - 0.35 * noise(x + 40, y, seed + 7): c = lerp(c, hexc(mark_col), 0.88)
            elif marks == "rosettes" and t < 0.95:
                cx, cy = (x // 4) * 4, (y // 4) * 4
                jr = random.Random(cx * 997 + cy * 31 + seed)
                ox, oy = cx + 1.5 + jr.uniform(-0.8, 0.8), cy + 1.5 + jr.uniform(-0.8, 0.8)
                d = math.hypot(x - ox, y - oy)
                if 1.0 <= d < 1.9 and jr.random() < 0.85: c = lerp(c, hexc(mark_col), 0.85)
                elif d < 1.0: c = mul(c, 0.86)
            if spots and spot_col and l < spots: c = hexc(spot_col)
            px[x, y] = c + (a,)
    return img


def save(img, name):
    img.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    # Wild boar: coarse dark bristles, grizzled with grey-brown tips; the boar king near black.
    save(paint("hoglin/hoglin.png", ["140e0b", "2e221a", "5a4432", "8a7058"], 1.05, keep_light=215, seed=11, grain=0.09, streak=0.16,
               tips="a89478"), "wild_boar")
    save(paint("hoglin/hoglin.png", ["080606", "1a1210", "34261e", "5a4636"], 1.15, keep_light=205, seed=12, grain=0.08, streak=0.14,
               tips="7a6a5a"), "boar_king")
    # Grey wolf: a dark saddle fading to buff legs and a pale throat, grizzled.
    save(paint("wolf/wolf_angry.png", ["1e1e20", "4a4a4c", "8a8478", "d2cab8"], 0.9, seed=21, grain=0.08, tips="c8c0b0"), "grey_wolf")
    save(paint("wolf/wolf_angry.png", ["3a3e48", "8a90a0", "d4d8e2", "fbfbfd"], 0.75, seed=22, grain=0.05, streak=0.07, tips="ffffff"), "wolf_king")
    # Tigers: rust to orange to cream belly, with wandering black stripes.
    save(paint("cat/ocelot.png", ["4a1c06", "b4581a", "e08a2e", "f6e6cc"], 0.9, spots=60, seed=31, marks="stripes", mark_col="140e0a"), "tiger")
    save(paint("cat/ocelot.png", ["7a7a84", "c4c4cc", "eeeef0", "ffffff"], 0.7, spots=60, seed=32, grain=0.05, marks="stripes",
               mark_col="26262e"), "white_tiger")
    # Leopard: golden with dark rosettes.
    save(paint("cat/ocelot.png", ["6a4410", "c08a2a", "e8c060", "f6ead0"], 0.85, spots=60, seed=41, marks="rosettes", mark_col="1e160e"), "leopard")
    # Bears: the snow bear's cream fur given grain; the black bear near black.
    save(paint("bear/polarbear.png", ["a8a49a", "d8d4c8", "eeebe2", "fbfaf6"], 1.0, seed=51, grain=0.04, streak=0.06), "snow_bear")
    save(paint("bear/polarbear.png", ["060505", "0e0b0a", "17120f", "241c17"], 1.0, seed=52, grain=0.08, streak=0.12, tips="4a3a30"), "black_bear")
    # Wild ox (a gaur): deep brown-black, lighter brow; horns kept.
    save(paint("cow/cow.png", ["0e0a08", "1e140e", "2c1e16", "3e2c20"], 0.9, keep_light=235, seed=61, grain=0.07, streak=0.1), "wild_ox")
    print("entity textures written")


if __name__ == "__main__":
    main()
