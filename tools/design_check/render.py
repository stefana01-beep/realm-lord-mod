import re, sys, math
from PIL import Image, ImageDraw, ImageFont

COLORS = [
    ("grass_block", (95,159,53)), ("dirt_path", (148,121,65)), ("dirt", (134,96,67)), ("stone_brick", (122,121,122)),
    ("andesite", (136,136,136)), ("cobblestone", (110,110,110)), ("stone", (125,125,125)), ("deepslate_tile", (54,54,58)),
    ("white_terracotta", (210,178,161)), ("stripped_mangrove", (120,54,48)), ("dark_oak", (66,43,20)),
    ("stripped_spruce", (115,89,52)), ("spruce", (114,84,48)), ("oak_planks", (162,130,78)), ("oak", (162,130,78)),
    ("glass", (190,220,235)), ("red_bed", (180,40,40)), ("white_bed", (235,235,235)), ("yellow_bed", (220,190,60)),
    ("red_carpet", (160,39,34)), ("red_wall_banner", (170,30,30)), ("chest", (170,120,40)), ("barrel", (130,95,50)),
    ("bookshelf", (110,80,50)), ("lantern", (255,200,90)), ("gold_block", (250,210,60)), ("ladder", (150,115,60)),
    ("water", (60,100,220)), ("lectern", (150,110,60)), ("cartography", (100,80,60)), ("enchanting", (60,40,90)),
    ("brewing", (90,90,90)), ("anvil", (70,70,70)), ("smithing", (60,60,70)), ("grindstone", (140,140,140)),
    ("crafting", (140,100,60)), ("lords_manor", (220,180,40)), ("potted", (60,140,60)), ("cauldron", (70,70,70)),
    ("wall", (120,120,120)), ("torch", (255,220,100)), ("poppy", (200,30,30)), ("daisy", (240,240,200)), ("sapling", (60,140,50)),
]

def color(name):
    for k, c in COLORS:
        if k in name: return c
    return (200, 0, 200)

def load(path):
    out = []
    for line in open(path):
        p = line.split(' ', 3)
        if len(p) < 4: continue
        name = re.search(r'minecraft:([a-z_]+)|realmlord:([a-z_]+)', p[3])
        n = name.group(1) or name.group(2)
        out.append((int(p[0]), int(p[1]), int(p[2]), n, p[3].strip()))
    return out

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)

def iso(blocks, out, scale=8, title="", cut=None):
    if cut is not None: blocks = [b for b in blocks if b[1] <= cut]
    if not blocks: return
    # view from +x,+z corner (south-east), looking north-west
    def proj(x, y, z):
        return ((x - z) * scale, (x + z) * scale / 2 - y * scale)
    pts = [proj(x+dx, y+dy, z+dz) for x,y,z,_,_ in blocks for dx in (0,1) for dy in (0,1) for dz in (0,1)]
    minx = min(p[0] for p in pts); maxx = max(p[0] for p in pts); miny = min(p[1] for p in pts); maxy = max(p[1] for p in pts)
    W, H = int(maxx - minx) + 40, int(maxy - miny) + 60
    img = Image.new('RGB', (W, H), (235, 232, 222))
    d = ImageDraw.Draw(img)
    ox, oy = -minx + 20, -miny + 40
    occ = {(x, y, z) for x, y, z, _, _ in blocks}
    for x, y, z, n, full in sorted(blocks, key=lambda b: (b[0] + b[2], b[1])):
        c = color(n)
        thin = any(k in n for k in ("carpet", "slab", "pane", "fence", "ladder", "lantern", "torch", "banner", "pot", "door", "bed", "trapdoor", "wall"))
        h = 0.5 if ("slab" in n or "bed" in n) else 1.0
        if "carpet" in n: h = 0.1
        def P(a, b, cc): 
            q = proj(a, b, cc); return (q[0] + ox, q[1] + oy)
        top = [P(x, y+h, z), P(x+1, y+h, z), P(x+1, y+h, z+1), P(x, y+h, z+1)]
        right = [P(x+1, y, z), P(x+1, y+h, z), P(x+1, y+h, z+1), P(x+1, y, z+1)]
        left = [P(x, y, z+1), P(x+1, y, z+1), P(x+1, y+h, z+1), P(x, y+h, z+1)]
        if (x+1, y, z) not in occ or thin: d.polygon(right, fill=shade(c, 0.75), outline=shade(c, 0.5))
        if (x, y, z+1) not in occ or thin: d.polygon(left, fill=shade(c, 0.9), outline=shade(c, 0.6))
        if (x, y+1, z) not in occ or thin: d.polygon(top, fill=c, outline=shade(c, 0.7))
    d.text((10, 10), title, fill=(0, 0, 0))
    img.save(out)

def plan(blocks, out, layer, title="", s=14):
    sel = {}
    for x, y, z, n, full in blocks:
        if y == layer: sel[(x, z)] = n
    if not sel: return
    xs = [k[0] for k in sel]; zs = [k[1] for k in sel]
    x0, z0 = min(xs), min(zs)
    W = (max(xs) - x0 + 1) * s + 20; H = (max(zs) - z0 + 1) * s + 40
    img = Image.new('RGB', (W, H), (240, 240, 240))
    d = ImageDraw.Draw(img)
    for (x, z), n in sel.items():
        X, Z = (x - x0) * s + 10, (z - z0) * s + 30
        d.rectangle([X, Z, X + s - 1, Z + s - 1], fill=color(n), outline=(80, 80, 80))
        d.text((X + 2, Z + 1), n[:2], fill=(0, 0, 0) if sum(color(n)) > 300 else (255, 255, 255))
    d.text((10, 8), title, fill=(0, 0, 0))
    img.save(out)

if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    import os
    os.makedirs(dst, exist_ok=True)
    for f in ["manor_1", "manor_2", "manor_3", "small_residential_courtyard", "medium_residential_courtyard", "large_residential_courtyard"]:
        b = load(f"{src}/{f}.txt")
        iso(b, f"{dst}/{f}.png", title=f)
    for f in ["manor_2", "manor_3"]:
        b = load(f"{src}/{f}.txt")
        iso(b, f"{dst}/{f}_cut_ground.png", title=f + " ground floor", cut=3)
        iso(b, f"{dst}/{f}_cut_upper.png", title=f + " upper floor", cut=7)
        plan(b, f"{dst}/{f}_plan_y0.png", 0, f + " plan y0 (south at bottom)")
        plan(b, f"{dst}/{f}_plan_y5.png", 5, f + " plan y5")
    for f in ["medium_residential_courtyard", "large_residential_courtyard"]:
        b = load(f"{src}/{f}.txt")
        iso(b, f"{dst}/{f}_cut.png", title=f + " ground floor", cut=2)
        plan(b, f"{dst}/{f}_plan_y4.png", 4, f + " upper beds y4")
    for f in ["palisade", "town_wall"]:
        b = load(f"{src}/{f}.txt")
        iso(b, f"{dst}/{f}.png", scale=5, title=f)
        # close-up of the east gate and the tower area
        near = [k for k in b if k[0] >= 16]
        iso(near, f"{dst}/{f}_east.png", scale=12, title=f + " east: gate at 90/270? tower at x 23")
