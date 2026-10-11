"""Procedurally paints Fiat Lux's 2D textures (blocks, item sprites, particles, effect icons).

    python3 tools/texgen/build_textures.py

Particle sprites are painted white/grey so the particle's colour tint drives the final hue.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/fiatlux/textures"


def clamp(v):
    return max(0, min(255, int(v)))


def save(img, rel, mcmeta=None):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    if mcmeta:
        p.with_name(p.name + ".mcmeta").write_text(json.dumps(mcmeta, indent=1) + "\n")


def noise_grid(seed, n=16):
    r = random.Random(seed)
    return [[r.random() for _ in range(n)] for _ in range(n)]


def smooth(grid, x, y):
    n = len(grid)
    x0, y0 = int(x) % n, int(y) % n
    x1, y1 = (x0 + 1) % n, (y0 + 1) % n
    fx, fy = x - int(x), y - int(y)
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = grid[y0][x0] * (1 - fx) + grid[y0][x1] * fx
    b = grid[y1][x0] * (1 - fx) + grid[y1][x1] * fx
    return a * (1 - fy) + b * fy


def fbm(grids, x, y):
    return sum(smooth(g, x * (2 ** i) / 4, y * (2 ** i) / 4) / (2 ** i) for i, g in enumerate(grids)) / 1.875


# ----------------------------------------------------------------------------- blocks

def lux_block():
    frames = 8
    img = Image.new("RGBA", (16, 16 * frames))
    g = [noise_grid(s) for s in (1, 2, 3, 4)]
    for f in range(frames):
        t = f / frames
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = math.hypot(dx, dy) / 10.6
                ang = math.atan2(dy, dx)
                swirl = 0.5 + 0.5 * math.sin(ang * 3 + d * 9 - t * math.tau)
                n = fbm(g, x + t * 16, y)
                v = 1.0 - d * 0.45 + swirl * 0.12 + n * 0.1
                rim = 1 if x in (0, 15) or y in (0, 15) else 0
                img.putpixel((x, y + 16 * f), (clamp(255 * min(1, v + 0.05)), clamp(236 * v), clamp(170 * v - rim * 30), 255))
    save(img, "block/lux_block.png", {"animation": {"frametime": 3, "interpolate": True}})


def golden_street():
    """Rev 21:21 - pure gold, as it were transparent glass."""
    img = Image.new("RGBA", (16, 16))
    g = [noise_grid(s) for s in (11, 12, 13)]
    for y in range(16):
        for x in range(16):
            n = fbm(g, x, y)
            streak = 0.5 + 0.5 * math.sin((x + y) * 0.7)
            edge = x in (0, 15) or y in (0, 15)
            r, gg, b = 250, 204 + n * 30, 80 + streak * 50
            a = 235 if edge else clamp(120 + n * 50 + (60 if (x + y) % 9 == 0 else 0))
            if (x - y) % 11 == 0 and not edge:
                r, gg, b, a = 255, 250, 220, 220
            img.putpixel((x, y), (clamp(r), clamp(gg), clamp(b), a))
    save(img, "block/golden_street.png")


def jasper():
    """Rev 21:11/18 - 'a jasper stone, clear as crystal'; faceted pale jade-crystal."""
    img = Image.new("RGBA", (16, 16))
    r = random.Random(7)
    seeds = [(r.uniform(0, 16), r.uniform(0, 16), r.uniform(-0.12, 0.12)) for _ in range(9)]
    for y in range(16):
        for x in range(16):
            ds = sorted(((x + .5 - sx) ** 2 + (y + .5 - sy) ** 2, sh) for sx, sy, sh in seeds)
            edge = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0]) < 0.7
            base = 0.86 + ds[0][1]
            c = (196 * base, 236 * base, 214 * base)
            if edge:
                c = (238, 255, 246)
            if (x * 7 + y * 3) % 23 == 0:
                c = (255, 255, 255)
            img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255))
    save(img, "block/jasper.png")


def cloudstone():
    img = Image.new("RGBA", (16, 16))
    g = [noise_grid(s) for s in (21, 22, 23, 24)]
    for y in range(16):
        for x in range(16):
            n = fbm(g, x, y)
            v = 228 + n * 30 - (8 if (x + y * 3) % 13 == 0 else 0)
            img.putpixel((x, y), (clamp(v + 4), clamp(v + 2), clamp(v - 4 + n * 10), 255))
    save(img, "block/cloudstone.png")


def pearl():
    """Rev 21:21 - every several gate was of one pearl: iridescent nacre."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = math.sin(x * 0.6 + y * 0.35) * 0.5 + 0.5
            u = math.sin(y * 0.8 - x * 0.2) * 0.5 + 0.5
            c = (236 + 14 * t, 230 + 16 * u, 238 + 12 * (1 - t))
            if (x - 8) ** 2 + (y - 6) ** 2 < 6:
                c = tuple(min(255, v + 12) for v in c)
            img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255))
    save(img, "block/pearl.png")


def celestial_crystal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shards = [(8, 1, 2.2), (4, 5, 1.6), (12, 4, 1.5), (6, 8, 1.2), (11, 9, 1.3)]
    for cx, top, w in shards:
        for y in range(top, 16):
            half = w * (1 - (y - top) / (16 - top) * 0.35) if y > top + 2 else w * (y - top + 0.5) / 2.5
            for x in range(16):
                if abs(x + 0.5 - cx) <= half:
                    shade = 0.82 + 0.18 * (1 if x < cx else 0.6)
                    c = (200 * shade, 238 * shade, 255 * shade)
                    if y == top or abs(x + 0.5 - cx) > half - 0.6:
                        c = (245, 252, 255)
                    img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 230))
    save(img, "block/celestial_crystal.png")


def stilled_water():
    frames = 16
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        t = f / frames * math.tau
        for y in range(16):
            for x in range(16):
                ripple = math.sin(y * 0.8 + t) * 0.5 + math.sin(x * 0.5 - t * 2 + y * 0.3) * 0.5
                c = (52 + ripple * 18, 112 + ripple * 26, 220 + ripple * 20)
                a = 170 + int(ripple * 25)
                img.putpixel((x, y + 16 * f), (clamp(c[0]), clamp(c[1]), clamp(c[2]), clamp(a)))
    save(img, "block/stilled_water.png", {"animation": {"frametime": 2, "interpolate": True}})


# ----------------------------------------------------------------------------- items

def divine_scepter():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # shaft from bottom-left to the head at upper right
    for i in range(10):
        x, y = 2 + i, 13 - i
        img.putpixel((x, y), (214, 160, 52, 255))
        img.putpixel((x + 1, y), (250, 214, 110, 255))
        if i % 3 == 0:
            img.putpixel((x, y + 1), (150, 100, 30, 255))
    # sun-head: gem with radiating rays
    cx, cy = 12, 3
    for a in range(8):
        ang = a * math.pi / 4
        for r in (2, 3):
            x, y = round(cx + math.cos(ang) * r), round(cy + math.sin(ang) * r)
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (255, 236, 150, 255) if r == 2 else (255, 250, 210, 200))
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            img.putpixel((cx + dx, cy + dy), (255, 255, 230, 255) if (dx, dy) == (0, 0) else (120, 190, 255, 255))
    img.putpixel((1, 14), (250, 214, 110, 255))
    save(img, "item/divine_scepter.png")


# ----------------------------------------------------------------------------- particles

def soft_disc(size, falloff=2.0, core=0.0):
    img = Image.new("RGBA", (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            a = max(0.0, 1 - d) ** falloff
            if d < core:
                a = 1.0
            img.putpixel((x, y), (255, 255, 255, clamp(a * 255)))
    return img


def star(size, points, sharp):
    img = Image.new("RGBA", (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            d = math.hypot(dx, dy) / (size / 2)
            ang = math.atan2(dy, dx)
            ray = abs(math.cos(ang * points / 2)) ** sharp
            a = max(0.0, 1 - d / (0.35 + 0.65 * ray)) ** 1.5
            img.putpixel((x, y), (255, 255, 255, clamp(a * 255)))
    return img


def flame_frame(f, frames=8, size=16):
    img = Image.new("RGBA", (size, size))
    t = f / frames
    g = noise_grid(40 + f)
    for y in range(size):
        for x in range(size):
            u = (x + 0.5) / size - 0.5
            v = (y + 0.5) / size
            width = 0.42 * (v ** 0.8) * (1 - t * 0.5)
            wob = math.sin(v * 9 + t * math.tau) * 0.05 * (1 - v)
            d = abs(u - wob) / max(0.01, width)
            n = smooth(g, x * 0.6, y * 0.6 + t * 8)
            a = max(0.0, 1 - d) * (0.6 + 0.4 * n) * min(1.0, v * 3)
            core = max(0.0, 1 - d * 2) * v
            bright = 200 + 55 * core
            img.putpixel((x, y), (255, clamp(bright), clamp(bright - 40 + core * 40), clamp(a * 255)))
    return img


def feather_sprite(variant, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    tilt = (-0.6, 0.0, 0.5)[variant]
    for i in range(13):
        t = i / 12
        cx = 8 + tilt * (t - 0.5) * 8
        cy = 2 + t * 12
        half = 3.2 * math.sin(t * math.pi) ** 0.7
        for x in range(size):
            dx = x + 0.5 - cx
            if abs(dx) <= half:
                barb = 230 + (12 if (x + i) % 2 else 0)
                img.putpixel((x, int(cy)), (barb, barb, 245, 255))
        img.putpixel((int(cx), int(cy)), (220, 190, 120, 255))
    return img


def shard_sprite(variant, size=8):
    """A jagged fragment of a broken halo: bright gold edge, darker core, one hot glint."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    polys = (((1, 1), (6, 2), (4, 7)), ((2, 0), (7, 4), (3, 6), (0, 3)), ((0, 2), (7, 1), (5, 4), (1, 6)))[variant]
    def inside(px, py):
        n, c = len(polys), False
        for k in range(n):
            (x0, y0), (x1, y1) = polys[k], polys[(k + 1) % n]
            if (y0 > py) != (y1 > py) and px < (x1 - x0) * (py - y0) / (y1 - y0) + x0:
                c = not c
        return c
    for x in range(size):
        for y in range(size):
            if inside(x + 0.5, y + 0.5):
                edge = not all(inside(x + 0.5 + dx, y + 0.5 + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                img.putpixel((x, y), (255, 236, 150, 255) if edge else (214, 160, 50, 255))
    gx, gy = polys[0]
    img.putpixel((min(size - 1, gx + 1), min(size - 1, gy + 1)), (255, 255, 240, 255))
    return img


def particles():
    for i in range(3):
        save(shard_sprite(i), f"particle/halo_shard_{i}.png")
    for i, (pts, sharp) in enumerate(((4, 6), (6, 5), (8, 8), (4, 2))):
        save(star(8, pts, sharp), f"particle/holy_spark_{i}.png")
    for f in range(8):
        save(flame_frame(f), f"particle/divine_flame_{f}.png")
    for i, (fo, core) in enumerate(((1.5, 0.0), (2.5, 0.1), (3.5, 0.2))):
        save(soft_disc(8, fo, core), f"particle/light_mote_{i}.png")
    for i in range(3):
        save(feather_sprite(i), f"particle/feather_{i}.png")
    for i, s in enumerate((4, 6, 8)):
        save(soft_disc(s, 1.2, 0.3), f"particle/ember_{i}.png")
    save(soft_disc(32, 2.2), "particle/radiance_0.png")
    save(soft_disc(32, 1.4), "particle/radiance_1.png")


# ----------------------------------------------------------------------------- effect icons

def icon(draw_fn, name):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw_fn(img)
    save(img, f"mob_effect/{name}.png")


def icon_blessed(img):
    for a in range(12):
        ang = a * math.pi / 6
        for r in range(5, 9):
            img.putpixel((round(9 + math.cos(ang) * r) % 18, round(9 + math.sin(ang) * r) % 18), (255, 214, 90, 255 - r * 18))
    for y in range(18):
        for x in range(18):
            if math.hypot(x - 8.5, y - 8.5) < 4:
                img.putpixel((x, y), (255, 244, 180, 255))


def icon_sanctified(img):
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, (y - 10) * 0.7)
            if d < 6 and y > 2:
                v = 255 - int(d * 12)
                img.putpixel((x, y), (v, v, 255, 255))
    for y in range(3, 15):
        img.putpixel((9, y), (255, 236, 150, 255))


def icon_cursed(img):
    for i in range(60):
        t = i / 60 * 4 * math.pi
        r = 1 + t * 0.55
        x, y = round(9 + math.cos(t) * r), round(9 + math.sin(t) * r)
        if 0 <= x < 18 and 0 <= y < 18:
            img.putpixel((x, y), (120, 50, 120, 255))


def icon_corrupted(img):
    r = random.Random(5)
    for y in range(2, 16):
        for x in range(2, 16):
            if math.hypot(x - 8.5, y - 8.5) < 7:
                crack = (x * 3 + y * 5 + r.randint(0, 2)) % 9 == 0
                img.putpixel((x, y), (230, 60, 30, 255) if crack else (70, 18, 18, 255))


def icon_spirit(img):
    f = flame_frame(3, size=18)
    for y in range(18):
        for x in range(18):
            r_, g_, b_, a = f.getpixel((x, y))
            img.putpixel((x, y), (255, clamp(g_ * 0.75), clamp(b_ * 0.3), a))


def main():
    lux_block(); golden_street(); jasper(); cloudstone(); pearl(); celestial_crystal(); stilled_water()
    divine_scepter()
    particles()
    icon(icon_blessed, "blessed"); icon(icon_sanctified, "sanctified"); icon(icon_cursed, "cursed")
    icon(icon_corrupted, "corrupted"); icon(icon_spirit, "spirit_filled")
    print("textures written to", ROOT)


if __name__ == "__main__":
    main()


# ----------------------------------------------------------------------------- underworld & worlds

def mottled(name, base, var, seed, speck=None, speck_rate=0):
    img = Image.new("RGBA", (16, 16))
    g = [noise_grid(s) for s in (seed, seed + 1, seed + 2)]
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            n = fbm(g, x, y)
            c = [base[i] + (n - 0.5) * var[i] for i in range(3)]
            if speck and r.random() < speck_rate:
                c = speck
            img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255))
    save(img, f"block/{name}.png")


def neon(name, rgb):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            tube = y in (5, 10) or x in (3, 12)
            core = (y in (5, 10) and 2 < x < 13) or (x in (3, 12) and 2 < y < 13)
            if edge:
                c = (30, 28, 34)
            elif core:
                c = (255, min(255, rgb[1] + 120), min(255, rgb[2] + 120))
            elif tube:
                c = rgb
            else:
                c = tuple(int(v * 0.25) + 10 for v in rgb)
            img.putpixel((x, y), (*c, 255))
    save(img, f"block/{name}.png")


def underworld_textures():
    mottled("sulfur", (226, 200, 64), (60, 60, 30), 301, (255, 246, 160), 0.05)
    mottled("brimstone", (120, 46, 20), (80, 40, 20), 311, (255, 170, 40), 0.12)
    mottled("abyssal_stone", (34, 24, 44), (30, 20, 40), 321, (90, 60, 120), 0.03)
    neon("neon_crimson", (230, 30, 70))
    neon("neon_violet", (150, 50, 240))
    # soul engine: riveted steel casing around a cyan soul-glow core
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.5:
                c = (120, 240, 255) if d < 2 else (40, 160, 200)
            elif (x in (1, 14) and y % 4 == 1) or (y in (1, 14) and x % 4 == 1):
                c = (180, 180, 190)
            else:
                c = (70, 72, 80) if (x + y) % 5 else (60, 62, 70)
            img.putpixel((x, y), (*c, 255))
    save(img, "block/soul_engine.png")
    mottled("regolith", (176, 174, 168), (40, 40, 40), 331, (120, 118, 112), 0.08)
    for name, base, seed in (("cloud_band_amber", (230, 170, 90), 341), ("cloud_band_cream", (240, 226, 196), 351), ("cloud_band_rust", (180, 96, 60), 361)):
        img = Image.new("RGBA", (16, 16))
        g = [noise_grid(s) for s in (seed, seed + 1)]
        for y in range(16):
            for x in range(16):
                band = math.sin((y + fbm(g, x, y) * 6) * 0.8) * 0.5 + 0.5
                c = [base[i] * (0.85 + 0.2 * band) for i in range(3)]
                img.putpixel((x, y), (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255))
        save(img, f"block/{name}.png")


if __name__ == "__main__":
    underworld_textures()


# ----------------------------------------------------------------------------- the Void

def void_gate():
    """The veil of a gate of pearl: deep starry dark seen through a flowing shimmer of pearl and
    gold. Built from functions periodic over the tile, so a wall of veil blocks reads as one
    seamless curtain (8 animated frames)."""
    from PIL import Image
    n, frames = 16, 8
    img = Image.new("RGBA", (n, n * frames))
    r = random.Random(7)
    stars = [(r.randrange(n), r.randrange(n), r.random()) for _ in range(7)]
    tau = math.tau
    for f in range(frames):
        t = f / frames
        for y in range(n):
            for x in range(n):
                u, v = x / n, y / n
                flow = (math.sin(tau * (u + 2 * v) + tau * t) + math.sin(tau * (2 * u - v) - tau * t)
                        + 0.6 * math.sin(tau * (3 * v) + tau * t * 2)) / 2.6
                k = 0.45 + 0.4 * flow
                base, pearl, gold = (18, 16, 46), (238, 232, 255), (255, 214, 120)
                c = [base[i] + (pearl[i] - base[i]) * k for i in range(3)]
                g = 0.5 + 0.5 * math.sin(tau * (u - v) + tau * t)
                c = [c[i] + (gold[i] - c[i]) * 0.3 * g for i in range(3)]
                img.putpixel((x, y + f * n), (clamp(c[0]), clamp(c[1]), clamp(c[2]), clamp(150 + 80 * k)))
        for sx, sy, ph in stars:
            tw = 0.5 + 0.5 * math.sin((t + ph) * tau)
            img.putpixel((sx, sy + f * n), (255, 255, 240, clamp(150 + 105 * tw)))
    save(img, "block/void_gate.png", {"animation": {"frametime": 3, "interpolate": True}})


if __name__ == "__main__":
    void_gate()
