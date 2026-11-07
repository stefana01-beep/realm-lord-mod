"""Paints 64x64 player-format skins for Realm Lord's human mobs (bandits and settlers).

The standard skin layout: every body part is a box whose six faces are unwrapped at a
texture offset (u, v) as
    top (u+d, v)       bottom (u+d+w, v)
    right (u, v+d)     front (u+d, v+d)     left (u+d+w, v+d)     back (u+2d+w, v+d)
"""
import random

from PIL import Image

# Base layer and overlay offsets: (u, v, w, h, d)
HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
JACKET = (16, 32, 8, 12, 4)
R_ARM = (40, 16, 4, 12, 4)
L_ARM = (32, 48, 4, 12, 4)
R_LEG = (0, 16, 4, 12, 4)
L_LEG = (16, 48, 4, 12, 4)


def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def faces(box):
    u, v, w, h, d = box
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def put(self, x, y, c, noise=0.06):
        if c is None:
            return
        if c[3] and noise:
            c = shade(c, 1.0 + (self.rnd.random() * 2 - 1) * noise)
        self.px[x, y] = c

    def box(self, box, fn, noise=0.06):
        """fn(face, x, y, w, h) -> colour or None, with x, y local to the face."""
        for name, (fx, fy, fw, fh) in faces(box).items():
            for y in range(fh):
                for x in range(fw):
                    self.put(fx + x, fy + y, fn(name, x, y, fw, fh), noise)


# --------------------------------------------------------------------------- body parts

def paint_head(p, skin, hair, eyes=rgb("2a1d16"), style="short", mask=None, band=None, beard=None):
    white = rgb("f2eee6")

    def fn(face, x, y, w, h):
        if face == "top":
            return hair
        if face == "bottom":
            return shade(skin, 0.8)
        hair_rows = {"short": 2, "long": 2, "bald": 0}[style]
        if face == "back":
            rows = 8 if style == "long" else 6
            if y < rows:
                return hair
            return shade(skin, 0.85)
        if face in ("left", "right"):
            if y < (6 if style == "long" else 3):
                return hair
            if style == "long" and y < 7 and (x == 0 if face == "left" else x == w - 1):
                return hair
            c = shade(skin, 0.9)
            if mask and y >= 5:
                return mask
            if beard and y >= 5 and (x >= 4 if face == "right" else x <= 3):
                return beard
            return c
        # front
        if y < hair_rows:
            return hair
        if band and y == 2:
            return band
        if y == 4 and x in (1, 6):
            return white
        if y == 4 and x in (2, 5):
            return eyes
        if y == 3 and x in (1, 2, 5, 6):
            return shade(hair, 1.1)                 # brows
        if mask and y >= 5:
            return shade(mask, 0.9 if y == 5 else 1.0)
        if beard and (y >= 6 or (y == 5 and x in (1, 2, 5, 6))):
            return beard
        if y == 6 and x in (3, 4):
            return shade(skin, 0.72)                 # mouth
        if y == 5 and x in (3, 4):
            return shade(skin, 0.9)                  # nose shadow
        return skin

    p.box(HEAD, fn)
    if band:
        # Headband wraps the base layer at the brow on every side.
        for name, (fx, fy, fw, fh) in faces(HEAD).items():
            if name in ("left", "right", "back"):
                for x in range(fw):
                    p.put(fx + x, fy + 2, band)


def paint_overlay_hair(p, hair, style):
    """Topknot or bun on the overlay layer: a small patch that stands proud of the head."""
    fx, fy, fw, fh = faces(HAT)["top"]
    if style in ("short", "bald"):
        for (x, y) in [(3, 3), (4, 3), (3, 4), (4, 4)]:
            p.put(fx + x, fy + y, shade(hair, 0.9))
    if style == "long":
        bx, by, bw, bh = faces(HAT)["back"]
        for (x, y) in [(3, 1), (4, 1), (2, 2), (3, 2), (4, 2), (5, 2), (3, 3), (4, 3)]:
            p.put(bx + x, by + y, shade(hair, 0.9))
        # a hairpin
        p.put(bx + 6, by + 2, rgb("c9a648"))


def paint_body(p, robe, sash, inner=None, trim=None, collar=True):
    inner = inner or shade(robe, 1.25)

    def fn(face, x, y, w, h):
        if face == "top":
            return shade(robe, 1.05)
        if face == "bottom":
            return shade(robe, 0.7)
        if y in (7, 8):
            if trim and y == 7:
                return trim
            return sash
        if face == "front" and collar and y <= 6:
            # Cross collar: the left lapel folds over the right, Hanfu style.
            edge = 3 + (y // 2)
            if x == edge or x == edge - 1 and y <= 1:
                return trim or inner
            if y <= 1 and 3 <= x <= 4:
                return inner
        if trim and face == "front" and y >= 9 and x == 3 + 3:
            return trim
        if y == h - 1:
            return shade(robe, 0.85)
        return robe

    p.box(BODY, fn)


def paint_arm(p, box, robe, skin, cuff=None, bracer=None, fur=None):
    def fn(face, x, y, w, h):
        if face == "top":
            return fur or shade(robe, 1.05)
        if face == "bottom":
            return skin
        if fur and y <= 1:
            return fur
        if y >= 10:
            return skin
        if bracer and 6 <= y <= 9:
            return bracer if y != 6 else shade(bracer, 1.2)
        if y == 9:
            return cuff or shade(robe, 1.25)
        return robe

    p.box(box, fn)


def paint_leg(p, box, robe, trousers, shoes, wrap=None):
    def fn(face, x, y, w, h):
        if face == "top":
            return robe
        if face == "bottom":
            return shade(shoes, 0.8)
        if y >= 10:
            return shoes
        if wrap and 7 <= y <= 9:
            return wrap if (y + x) % 2 == 0 else shade(wrap, 0.85)
        if y <= 4:
            return robe if face != "back" or y <= 3 else shade(robe, 0.9)
        return trousers

    p.box(box, fn)


def person(seed, skin, hair, style, robe, sash, trousers, shoes, inner=None, trim=None, mask=None, band=None,
           beard=None, bracer=None, fur=None, wrap=None, cuff=None):
    p = Painter(seed)
    paint_head(p, skin, hair, style=style, mask=mask, band=band, beard=beard)
    paint_overlay_hair(p, hair, style)
    paint_body(p, robe, sash, inner=inner, trim=trim)
    paint_arm(p, R_ARM, robe, skin, cuff=cuff, bracer=bracer, fur=fur)
    paint_arm(p, L_ARM, robe, skin, cuff=cuff, bracer=bracer, fur=fur)
    paint_leg(p, R_LEG, robe, trousers, shoes, wrap=wrap)
    paint_leg(p, L_LEG, robe, trousers, shoes, wrap=wrap)
    return p.img


SKINS = [rgb("e9c4a0"), rgb("dbb087"), rgb("c4966e"), rgb("aa7d5a")]
BLACK_HAIR = rgb("1e1916")
BROWN_HAIR = rgb("37261c")
GREY_HAIR = rgb("7a756e")


def bandits():
    out = {}
    robes = [("3a2d26", "8c1f1c"), ("2a2a2e", "6b4a2a"), ("4a3a28", "8c1f1c"), ("30382c", "5a3b22")]
    for i, (robe, sash) in enumerate(robes):
        out[f"bandit_{i}"] = person(
            100 + i, SKINS[(i + 1) % 4], BLACK_HAIR if i % 2 == 0 else BROWN_HAIR, "short",
            rgb(robe), rgb(sash), rgb("2b2420"), rgb("1c1714"),
            inner=rgb("6a5a48"), mask=rgb("1d1b1b") if i % 2 == 0 else None,
            band=rgb("9c2420") if i % 2 == 1 else None, wrap=rgb("8a7a60"))
    for i, robe in enumerate(["40482f", "4b3d2a"]):
        out[f"bandit_archer_{i}"] = person(
            200 + i, SKINS[i * 2], BROWN_HAIR, "short", rgb(robe), rgb("5a3b22"), rgb("2f2a22"), rgb("1c1714"),
            inner=rgb("7a7050"), band=rgb("2e4a2e") if i == 0 else rgb("9c2420"), mask=rgb("2a2a24") if i == 1 else None,
            bracer=rgb("6b4426"), wrap=rgb("8a7a60"))
    out["bandit_chief"] = person(
        300, SKINS[2], BLACK_HAIR, "short", rgb("1a1719"), rgb("8c1f1c"), rgb("231d1d"), rgb("120f0e"),
        inner=rgb("9c2420"), trim=rgb("c9a13e"), band=rgb("9c2420"), beard=rgb("1b1512"),
        fur=rgb("6e655a"), bracer=rgb("3b2a1e"), cuff=rgb("c9a13e"))
    return out


def raiders():
    """Water bandits of the rivers and lakes (bare-armed boatmen in blue-grey with head cloths) and
    the barbarians of the northern steppe (braided hair, fur and leather, heavy beards)."""
    out = {}
    for i, (robe, band) in enumerate([("3e4a56", "c8c0a8"), ("2f3d3a", "9c2420")]):
        out[f"water_bandit_{i}"] = person(500 + i, SKINS[2 + i], BLACK_HAIR, "short", rgb(robe), rgb("6a5a40"), rgb("2e3a44"),
                                          rgb("8a7450"), inner=rgb("c8c0a8"), band=rgb(band), wrap=rgb("c8c0a8"), cuff=SKINS[2 + i])
    out["water_bandit_chief"] = person(520, SKINS[3], BLACK_HAIR, "long", rgb("1e2c3a"), rgb("2a6a8a"), rgb("1a2028"), rgb("120f0e"),
                                       inner=rgb("5a7a8a"), trim=rgb("c9a13e"), band=rgb("2a6a8a"), beard=rgb("1b1512"),
                                       bracer=rgb("3b2a1e"))
    for i, (robe, fur) in enumerate([("6a4a2e", "8a7a62"), ("4a3a2a", "5a4a3a"), ("7a5a38", "c0b090")]):
        out[f"barbarian_{i}"] = person(600 + i, SKINS[(i + 1) % 4], BROWN_HAIR if i == 1 else BLACK_HAIR, "long", rgb(robe), rgb("2a1d16"),
                                       rgb("3a2a1e"), rgb("2a1d16"), inner=rgb(fur), trim=rgb(fur), beard=rgb("2a1d16"),
                                       fur=rgb(fur), wrap=rgb("5a4a3a"))
    # Mountain barbarians of the southern hills: dark skin, hide wraps, bright headbands.
    for i, (robe, band) in enumerate([("5a3a22", "c03020"), ("4a4028", "2a8a4a"), ("6a4a2a", "d8a020")]):
        out[f"mountain_barbarian_{i}"] = person(700 + i, SKINS[3], BLACK_HAIR, "long", rgb(robe), rgb("3a2a1a"), rgb("4a3a26"),
                                                rgb("2a1d16"), inner=SKINS[3], band=rgb(band), wrap=rgb("8a6a40"), cuff=SKINS[3])
    out["mountain_chief"] = person(720, SKINS[3], BLACK_HAIR, "long", rgb("3a2412"), rgb("c03020"), rgb("2a1a10"), rgb("120f0e"),
                                   inner=rgb("e8e0c8"), trim=rgb("c9a13e"), band=rgb("c03020"), beard=rgb("1b1512"),
                                   fur=rgb("c8b890"), bracer=rgb("c9a13e"))
    out["barbarian_chief"] = person(620, SKINS[3], BLACK_HAIR, "long", rgb("3a1e14"), rgb("c9a13e"), rgb("2a1a12"), rgb("120f0e"),
                                    inner=rgb("d8d0c0"), trim=rgb("d8d0c0"), band=rgb("c9a13e"), beard=rgb("1b1512"),
                                    fur=rgb("e8e0d0"), bracer=rgb("c9a13e"), cuff=rgb("c9a13e"))
    return out


def settlers():
    out = {}
    looks = [
        # skin, hair, style, robe, sash, trousers, shoes
        (0, BLACK_HAIR, "short", "b0a082", "6b5a40", "5a5046", "a8935c"),
        (1, BLACK_HAIR, "long", "3c4b6e", "b0a082", "34303a", "4a3a2a"),
        (2, BROWN_HAIR, "short", "6f7d5f", "4a3a28", "4a443a", "a8935c"),
        (0, BLACK_HAIR, "long", "8c4a3a", "d8c8a0", "3a302a", "4a3a2a"),
        (3, GREY_HAIR, "short", "8a8478", "4a3a28", "4a4640", "a8935c"),
        (1, BROWN_HAIR, "long", "6a7a8c", "3c4b6e", "3a3a40", "4a3a2a"),
        (2, BLACK_HAIR, "short", "7a5a3e", "b0a082", "4a3a2c", "a8935c"),
        (0, BLACK_HAIR, "long", "c0b8a8", "6a4a7a", "4a4448", "4a3a2a"),
    ]
    for i, (sk, hair, style, robe, sash, trousers, shoes) in enumerate(looks):
        out[f"settler_{i}"] = person(400 + i, SKINS[sk], hair, style, rgb(robe), rgb(sash), rgb(trousers), rgb(shoes),
                                     inner=shade(rgb(robe), 1.3), wrap=rgb("cfc2a0") if style == "short" else None)
    return out


def all_skins():
    d = bandits()
    d.update(raiders())
    d.update(settlers())
    return d
