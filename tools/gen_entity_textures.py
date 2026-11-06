"""The beasts' skins: recoloured from Minecraft's own animals (hoglin, wolf, ocelot, polar bear,
cow), whose UV layouts the renderers reuse. Usage: gen_entity_textures.py <vanilla textures/entity dir>"""
import os
import sys
from PIL import Image

SRC = sys.argv[1]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "realmlord", "textures", "entity")


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def tint(src, dark, light, gamma=1.0, keep_light=None, spots=None, stripes=None, stripe_col=None):
    """Maps each pixel's brightness onto dark..light. Pixels brighter than keep_light (eyes, tusks,
    horns) stay as they were; pixels darker than `spots` (an ocelot's spots) become stripe_col;
    `stripes` lays bands of stripe_col every few pixels across the pelt."""
    img = Image.open(os.path.join(SRC, src)).convert("RGBA")
    px = img.load()
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
            c = lerp(hexc(dark), hexc(light), t)
            if spots and l < spots: c = hexc(stripe_col)
            elif stripes and stripes > 0 and (x + y // 2) % stripes == 0 and t > 0.25: c = lerp(hexc(stripe_col), c, 0.2)
            elif stripes and stripes < 0 and (x * 3 + y * 5) % -stripes == 0 and t > 0.35: c = lerp(hexc(stripe_col), c, 0.15)
            px[x, y] = c + (a,)
    return img


def save(img, name):
    img.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    save(tint("hoglin/hoglin.png", "1e1612", "7a5a40", 1.1, keep_light=215), "wild_boar")
    save(tint("hoglin/hoglin.png", "0c0a0a", "4a3428", 1.2, keep_light=205), "boar_king")
    save(tint("wolf/wolf_angry.png", "26282c", "8c8e92", 0.9), "grey_wolf")
    save(tint("wolf/wolf_angry.png", "4a4e58", "f2f2f6", 0.7), "wolf_king")
    save(tint("cat/ocelot.png", "5a2a0a", "f0962e", 0.9, spots=60, stripes=4, stripe_col="16120e"), "tiger")
    save(tint("cat/ocelot.png", "6a4a18", "f0cc6a", 0.8, spots=60, stripes=-7, stripe_col="1e160e"), "leopard")
    save(tint("cat/ocelot.png", "9a9aa2", "fcfcfa", 0.6, spots=60, stripes=4, stripe_col="24242c"), "white_tiger")
    save(Image.open(os.path.join(SRC, "bear/polarbear.png")).convert("RGBA"), "snow_bear")
    save(tint("bear/polarbear.png", "0e0c0c", "3e3430", 1.4), "black_bear")
    save(tint("cow/cow.png", "1a120e", "8a6446", 0.9, keep_light=235), "wild_ox")
    print("entity textures written")


if __name__ == "__main__":
    main()
