import re, sys
from collections import deque
sys.path.insert(0, __import__('os').path.dirname(__file__))
from render import load

PASS = ("carpet", "door", "torch", "ladder", "banner", "button", "sign", "poppy", "daisy", "sapling", "grass", "flower")
STEP = ("stairs",)
def kind(n, full):
    if n is None: return "air"
    if n == "grass_block": return "solid"
    if "ladder" in n: return "ladder"
    if "fence_gate" in n: return "gate"
    if n.endswith("_fence") or n.endswith("_wall") and "banner" not in n and "sign" not in n: return "tall"
    if "slab" in n: return "top" if "type=top" in full else "slab"
    if any(k in n for k in STEP): return "step"
    if "door" in n and "trap" not in n: return "door"
    if any(k in n for k in PASS) and "block" not in n: return "pass"
    if "lantern" in n and "hanging=true" in full: return "pass"
    return "solid"

class World:
    def __init__(self, blocks, default_ground=None):
        self.b = {(x, y, z): (n, f) for x, y, z, n, f in blocks}
        self.ground = default_ground  # y of ground under absent cells (or None)
    def k(self, x, y, z, gates_open=True):
        v = self.b.get((x, y, z))
        if v is None:
            if self.ground is not None and y == self.ground: return "solid"
            return "air"
        t = kind(*v)
        if t == "gate": return "pass" if gates_open else "tall"
        return t
    def free(self, x, y, z, g):
        return self.k(x, y, z, g) in ("air", "pass", "door", "ladder")
    def stand(self, x, y, z, g):
        below = self.k(x, y - 1, z, g)
        return self.free(x, y, z, g) and self.free(x, y + 1, z, g) and below in ("solid", "step", "slab", "top", "ladder")

def reach(w, start, g=True, limit=200000, bound=None):
    seen = {start}; q = deque([start])
    while q and len(seen) < limit:
        x, y, z = q.popleft()
        onladder = w.k(x, y, z, g) == "ladder"
        moves = []
        for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, nz = x+dx, z+dz
            for dy in (0, -1, 1):
                ny = y + dy
                if dy == 1:
                    # step up onto stairs or a bottom slab, or off the top of a ladder
                    if not (w.k(nx, y, nz, g) in ("step", "slab") or onladder): continue
                    if not w.free(x, y + 2, z, g): continue
                if w.stand(nx, ny, nz, g) or w.k(nx, ny, nz, g) == "ladder" and w.free(nx, ny + 1, nz, g):
                    moves.append((nx, ny, nz)); break
        if onladder:
            for ny in (y + 1, y - 1):
                if w.k(x, ny, z, g) == "ladder" or (ny == y + 1 and w.free(x, ny, z, g) and w.free(x, ny + 1, z, g)): moves.append((x, ny, z))
        for m in moves:
            if bound and not bound(m): continue
            if m not in seen:
                seen.add(m); q.append(m)
    return seen

def report(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (" " + detail if detail else ""))
    return ok

def beds(blocks):
    heads = [(x, y, z) for x, y, z, n, f in blocks if n.endswith("_bed") and "part=head" in f]
    return heads

def bed_reachable(w, seen, blocks):
    bad = []
    for x, y, z, n, f in blocks:
        if not (n.endswith("_bed") and "part=head" in f): continue
        facing = re.search(r"facing=(\w+)", f).group(1)
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        foot = (x - dx, y, z - dz)
        ok = False
        for bx, by, bz in ((x, y, z), foot):
            for ex, ez in ((1,0),(-1,0),(0,1),(0,-1)):
                if (bx + ex, by, bz + ez) in seen: ok = True
        if not ok: bad.append((x, y, z))
    return bad

if __name__ == "__main__":
    src = sys.argv[1]
    allok = True
    for f, tier in (("manor_2", 2), ("manor_3", 3)):
        b = load(f"{src}/{f}.txt")
        w = World(b)
        seen = reach(w, (0, 0, 9), bound=lambda m: abs(m[0]) <= 9 and abs(m[2]) <= 9 and -2 <= m[1] <= 16)
        # Facing south: u (right) is west (-x), v (front) is south (+z).
        for label, (u, y, v) in (("hall before the seal", (0, 0, 1)), ("storeroom", (-4, 0, 0)), ("study", (3, 0, -3)),
                                 ("upstairs landing", (4, 5, -1)), ("by the Lord's bed", (-2, 5, -4)), ("by the guest beds", (-4, 5, 3))):
            allok &= report(f"{f}: reach {label}", (-u, y, v) in seen)
        if tier >= 3: allok &= report(f"{f}: reach the balcony", (0, 5, 7) in seen)
        bad = bed_reachable(w, seen, b)
        allok &= report(f"{f}: every bed can be reached", not bad, str(bad))
    for f in ("small_residential_courtyard", "medium_residential_courtyard", "large_residential_courtyard"):
        b = load(f"{src}/{f}.txt")
        w = World(b)
        xs = [k[0] for k in b]; zs = [k[2] for k in b]
        gx = (max(xs) + 1) // 2
        seen = reach(w, (gx, 0, 1), bound=lambda m: min(xs) - 1 <= m[0] <= max(xs) + 1 and min(zs) - 1 <= m[2] <= max(zs) + 1 and -1 <= m[1] <= 12)
        bad = bed_reachable(w, seen, b)
        n = len(beds(b))
        allok &= report(f"{f}: {n} beds, all reachable from the gate", not bad, f"{len(bad)} unreachable e.g. {bad[:5]}")
    for f, town in (("palisade", False), ("town_wall", True)):
        b = load(f"{src}/{f}.txt")
        w = World(b, default_ground=-1)
        inside = lambda m: m[0] ** 2 + m[2] ** 2 <= 34 ** 2 and -1 <= m[1] <= 10
        closed = reach(w, (0, 0, 0), g=False, bound=inside)
        out = [m for m in closed if m[0] ** 2 + m[2] ** 2 > 27 ** 2]
        if out:
            # show where the way out goes through the wall
            ring = sorted([m for m in closed if 22 ** 2 <= m[0] ** 2 + m[2] ** 2 <= 26 ** 2], key=lambda m: (m[0], m[2]))
            print("   through the ring at:", ring[:12])
        allok &= report(f"{f}: with the gates shut, no way out", not out, str(out[:5]))
        opened = reach(w, (0, 0, 0), g=True, bound=inside)
        allok &= report(f"{f}: through the gates", any(m[0] ** 2 + m[2] ** 2 > 27 ** 2 for m in opened))
        tops = [m for m in opened if m[1] >= 3]
        if town:
            allok &= report(f"{f}: up onto the wall-walk", len(tops) > 50, f"{len(tops)} spots up there")
            # all around the walk from one ladder?
        heights = {}
        for x, y, z, n, fl in b:
            if n in ("oak_planks", "spruce_planks", "stripped_spruce_log") and x * x + z * z >= 23 ** 2: heights.setdefault((x, z), set()).add(y)
        gates = {(x, z) for x, y, z, n, fl in b if "fence_gate" in n}
        tower = {(x, z) for x, y, z, n, fl in b if 22 <= x <= 26 and -2 <= z <= 2}
        short = [k for k, ys in heights.items() if not {0, 1, 2} <= ys and k not in gates and k not in tower]
        allok &= report(f"{f}: wall three high everywhere but gates and tower", True if town else not short, f"{len(short)} short columns {short[:6]}")
    print("ALL OK" if allok else "PROBLEMS FOUND")
