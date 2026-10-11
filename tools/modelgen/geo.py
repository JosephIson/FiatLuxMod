"""Procedural builder for GeckoLib / Blockbench "Bedrock Entity" models.

A Model is a tree of bones holding cubes. Each cube carries a material that is used
to paint its faces on an auto-packed texture atlas (plus a _glowmask atlas for
emissive materials, consumed by GeckoLib's AutoGlowingGeoLayer). Output files open
directly in Blockbench (File > Open Model) for hand polishing.

Coordinates follow Blockbench's Bedrock convention: pixels, y up, the model faces
north (-z), cube "origin" is the absolute min corner, bone "pivot" is absolute.
"""
from __future__ import annotations

import json
import math
import random
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw

# ----------------------------------------------------------------------------- materials

@dataclass(frozen=True)
class Material:
    name: str
    color: tuple            # base RGB
    pattern: str = "plain"  # plain, cloth, gold, feather, skin, hair, crystal, fire, eye, metal, wood, rune, steel, scale
    emissive: bool = False  # painted into the _glowmask atlas as well
    glow_veins: bool = False  # with emissive: only the vein/glyph texels glow (burning-coal look)
    alpha: int = 255
    accent: tuple | None = None


def _clamp(v):
    return max(0, min(255, int(v)))


def _shade(c, f):
    return tuple(_clamp(x * f) for x in c)


def _mix(a, b, t):
    return tuple(_clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


FACE_LIGHT = {"up": 1.12, "down": 0.72, "north": 1.0, "south": 0.9, "east": 0.95, "west": 0.95}
SIDES = ("north", "south", "east", "west")


def _key_pattern(i, j, period=6):
    """Greek-key / meander band used for embroidered hems."""
    a, b = i % period, j % period
    return (a == 0 and b < period - 1) or (b == 0 and a > 0) or (a == period - 2 and 1 < b < period - 1) or (b == period - 2 and 2 <= a <= period - 2)


_GLOW = [False]
_COAL_SEEDS = {}


def texel(mat: Material, face: str, i, j, w, h, rng: random.Random, vertical: bool, density: int):
    """Colour + alpha of one texel. (i, j) are texel coords inside a face of w x h texels."""
    u = (i + 0.5) / w
    v = (j + 0.5) / h
    edge = min(i, j, w - 1 - i, h - 1 - j)
    n = rng.uniform(-1, 1)
    c = mat.color
    a = mat.alpha
    accent = mat.accent or _shade(mat.color, 0.6)
    p = mat.pattern
    side = face in SIDES
    along = v if vertical else u          # coordinate along the long axis
    across = u if vertical else v

    if p == "plain":
        c = _shade(c, 1 + n * 0.04)
    elif p == "cloth":
        # soft folds from a sum of sines, slightly irregular per column
        fold = math.sin(across * math.pi * max(2.0, (w if vertical else h) / (2.5 * density))) * 0.06
        fold += math.sin(across * 17.0 + along * 2.0) * 0.025
        c = _shade(c, 1 + fold + n * 0.025)
        if side and vertical and h >= 6 * density:
            band = h - j - 1
            if band < 3 * density:  # embroidered meander hem
                c = accent if _key_pattern(i, band, 2 * density + 2) else _shade(c, 0.94)
    elif p == "gold":
        # bevelled plate with filigree scrollwork and specular glints
        scroll = math.sin(u * 9.4 + math.sin(v * 7.0) * 1.6) * math.sin(v * 9.4 + math.sin(u * 6.0) * 1.6)
        c = _shade(c, 1.0 + (0.18 if scroll > 0.55 else (-0.12 if scroll < -0.6 else 0)) + n * 0.03)
        if edge == 0:
            c = _shade(c, 0.72 if (j == h - 1 or i == w - 1) else 1.28)
        if (i * 5 + j * 3) % 23 == 0:
            c = _mix(c, (255, 252, 230), 0.7)
    elif p in ("metal", "steel"):
        brushed = math.sin(j * 2.3 + n * 0.3) * 0.05 + math.sin(i * 0.4) * 0.03
        hammered = -0.1 if (i * 7 + j * 11) % 17 == 0 else 0
        c = _shade(c, 1 + brushed + hammered + n * 0.02)
        if edge == 0:
            c = _shade(c, 0.7 if j == h - 1 else 1.3)
    elif p == "feather":
        # rachis down the middle, angled barbs, darker root, notched pale tip
        dist = abs(across - 0.5)
        shaft = dist < max(0.06, 0.5 / max(1, (w if vertical else h)))
        barb = ((i + j) if (across > 0.5) else (i - j)) % 3 == 0
        root = along if vertical else (1 - along)
        c = _shade(c, 0.86 + 0.18 * root + (0.07 if barb else -0.02) + n * 0.02 - dist * 0.12)
        if shaft:
            c = _mix(c, accent, 0.65)
        if root > 0.9 and dist > 0.3:
            a = 0 if (i + j) % 2 == 0 and mat.alpha == 255 and not side else a
            c = _mix(c, (255, 255, 255), 0.3)
    elif p == "scale":
        # overlapping scales in offset rows: lit crown, dark rim, and an iridescent sheen shifting along the body
        row = j // 3
        col = (i + (row % 2) * 2) // 4
        si, sj = (i + (row % 2) * 2) % 4, j % 3
        k = 1.12 if sj == 0 else (0.72 if sj == 2 or si == 3 else 1.0)
        sheen = 0.5 + 0.5 * math.sin(along * 9.0 + col * 0.9)
        c = _mix(_shade(c, k + n * 0.03), accent, 0.35 * sheen)
    elif p == "skin":
        c = _shade(c, 1 + n * 0.02 - (0.05 if edge == 0 else 0) + (0.04 if face == "up" else 0))
    elif p == "hair":
        strand = math.sin(across * (w if vertical else h) * 1.9 + n) * 0.08
        c = _shade(c, 0.95 + strand + 0.08 * (1 - along) + n * 0.02)
    elif p == "wood":
        grain = math.sin((u * 3 + v * 22) + math.sin(v * 7) * 1.5) * 0.12
        c = _shade(c, 1 + grain + n * 0.05)
    elif p == "crystal":
        # faceted: brightness by triangular facet bands, bright sparkles, clearer core
        facet = ((int(u * 4) + int(v * 3) + int((u + v) * 3)) % 3 - 1) * 0.09
        c = _shade(c, 1.02 + facet + n * 0.03)
        if (i * 7 + j * 13) % 31 == 0:
            c = (255, 255, 255)
        a = mat.alpha if edge > 0 else min(255, mat.alpha + 70)
    elif p == "fire":
        t = along if vertical else across
        lick = math.sin(across * 13 + n) * 0.12
        c = _mix((255, 252, 210), mat.color, min(1.0, max(0.0, (1 - t) + lick)))
        c = _mix(c, accent, max(0.0, (1 - t) - 0.65) * 1.6)
        if rng.random() < 0.03:
            c = (255, 255, 230)
    elif p == "eye":
        d = math.hypot(u - 0.5, v - 0.5)
        c = (250, 250, 255) if d > 0.36 else (_shade(mat.color, 0.8 + d * 1.2) if d > 0.14 else (18, 16, 26))
        if 0.22 < u < 0.32 and 0.22 < v < 0.32:
            c = (255, 255, 255)
    elif p == "rune":
        c = _shade(c, 0.85 + n * 0.05)
        glyph = (i % (2 * density + 1) == density and j % 2 == 0) or (j % (3 * density) == density and i % 2 == 1)
        if glyph or (edge == 1 and w > 3 and h > 3):
            c = accent
            _GLOW[0] = True
    elif p == "coal":
        # cracked coal (Ezek 1:13): Voronoi cells of dark coal outlined by continuous ember veins
        seeds = _COAL_SEEDS.get((w, h, face))
        if seeds is None:
            r2 = random.Random(w * 7919 + h * 104729 + hash(face) % 1000)
            k = max(3, (w * h) // 12)
            seeds = [(r2.uniform(0, w), r2.uniform(0, h)) for _ in range(k)]
            _COAL_SEEDS[(w, h, face)] = seeds
        ds = sorted((i + 0.5 - sx) ** 2 + (j + 0.5 - sy) ** 2 for sx, sy in seeds)
        gap = math.sqrt(ds[1]) - math.sqrt(ds[0]) if len(ds) > 1 else 9
        if gap < 0.5:
            c = _mix((255, 236, 160), accent, gap / 0.5)
            _GLOW[0] = True
        else:
            c = _shade(c, 0.82 + min(0.25, math.sqrt(ds[0]) * 0.05) + n * 0.05)

    elif p == "membrane":
        # leathery bat-wing membrane: dark skin stretched thin, branching ember-red veins,
        # thinner (more translucent) away from the edges
        vein = abs(math.sin(u * 3.0 + math.sin(v * 2.5) * 1.2)) < 0.06 or abs(math.sin(v * 4.0 + u * 1.5)) < 0.04
        c = _shade(c, 0.94 + n * 0.02 + (0.06 if edge > 1 else -0.1))
        if vein:
            c = _mix(c, accent, 0.75)
        if edge > 1 and mat.alpha < 255:
            a = mat.alpha
        else:
            a = 255
    if side and p not in ("eye", "fire", "rune", "coal"):
        c = _shade(c, 1.06 - 0.16 * v)          # ambient occlusion toward the bottom
        if j == 0:
            c = _shade(c, 1.08)                  # rim light on the top edge
    return _shade(c, FACE_LIGHT[face] if p != "eye" else 1.0), a


def paint_face(px, gpx, rect, mat, face, rng, vertical, density):
    """Paints one face; emissive texels are also written to the glowmask. Returns True if any glow."""
    x0, y0, w, h = rect
    glowed = False
    for j in range(h):
        for i in range(w):
            _GLOW[0] = False
            c, a = texel(mat, face, i, j, w, h, rng, vertical, density)
            px[x0 + i, y0 + j] = (*c, a)
            if mat.emissive and a > 0 and (_GLOW[0] or not mat.glow_veins):
                boost = 1.15 if mat.pattern in ("fire", "rune", "crystal", "coal") else 1.0
                gpx[x0 + i, y0 + j] = (_clamp(c[0] * boost), _clamp(c[1] * boost), _clamp(c[2] * boost), 255)
                glowed = True
    return glowed


# ----------------------------------------------------------------------------- geometry

@dataclass
class Cube:
    origin: tuple
    size: tuple
    material: Material
    inflate: float = 0.0
    rotation: tuple | None = None
    pivot: tuple | None = None
    uv: tuple | None = None  # allocated block (u, v)


@dataclass
class Bone:
    name: str
    pivot: tuple
    parent: str | None = None
    rotation: tuple = (0, 0, 0)
    cubes: list = field(default_factory=list)


class Model:
    def __init__(self, identifier: str, visible_bounds=(4, 4, (0, 1.5, 0)), density=2):
        self.identifier = identifier
        self.density = density  # texels per model pixel
        self.bones: dict[str, Bone] = {}
        self.visible_bounds = visible_bounds
        self.tex_w = self.tex_h = 0

    # -- building ----------------------------------------------------------------
    def bone(self, name, pivot, parent=None, rotation=(0, 0, 0)):
        if name in self.bones:
            raise ValueError(f"duplicate bone {name}")
        if parent is not None and parent not in self.bones:
            raise ValueError(f"bone {name}: unknown parent {parent}")
        self.bones[name] = Bone(name, tuple(pivot), parent, tuple(rotation))
        return name

    def cube(self, bone, origin, size, material, inflate=0.0, rotation=None, pivot=None):
        c = Cube(tuple(origin), tuple(size), material, inflate,
                 tuple(rotation) if rotation else None,
                 tuple(pivot) if pivot else None)
        self.bones[bone].cubes.append(c)
        return c

    def centered(self, bone, center, size, material, **kw):
        cx, cy, cz = center
        sx, sy, sz = size
        return self.cube(bone, (cx - sx / 2, cy - sy / 2, cz - sz / 2), size, material, **kw)

    def ring(self, bone, center, radius, segments, seg_size, material, axis="y", phase=0.0, **kw):
        """A ring of rotated segment cubes (halos, Ophanim wheels, crossguard rosettes)."""
        cx, cy, cz = center
        for k in range(segments):
            ang = 2 * math.pi * k / segments + phase
            deg = math.degrees(ang)
            if axis == "y":    # ring lying flat (halo)
                p = (cx + radius * math.cos(ang), cy, cz + radius * math.sin(ang))
                rot = (0, -deg + 90, 0)
            elif axis == "z":  # ring facing forward
                p = (cx + radius * math.cos(ang), cy + radius * math.sin(ang), cz)
                rot = (0, 0, -(deg + 90))   # GeckoLib mirrors x, which reverses z-rotation
            else:              # axis x
                p = (cx, cy + radius * math.cos(ang), cz + radius * math.sin(ang))
                rot = (-(deg + 90), 0, 0)   # GeckoLib negates x-rotation
            sx, sy, sz = seg_size
            self.cube(bone, (p[0] - sx / 2, p[1] - sy / 2, p[2] - sz / 2), seg_size, material,
                      rotation=rot, pivot=p, **kw)

    # -- stats -------------------------------------------------------------------
    def stats(self):
        return {"bones": len(self.bones), "cubes": sum(len(b.cubes) for b in self.bones.values())}

    # -- uv packing --------------------------------------------------------------
    def _dims(self, c: Cube):
        return tuple(max(1, math.ceil(s * self.density - 1e-6)) for s in c.size)

    def _pack(self):
        cubes = [c for b in self.bones.values() for c in b.cubes]
        blocks = []
        for c in cubes:
            w, h, d = self._dims(c)
            blocks.append((c, 2 * (d + w), d + h))
        blocks.sort(key=lambda t: (-t[2], -t[1]))
        for size in (64, 128, 256, 512, 1024, 2048):
            x = y = row_h = 0
            ok = True
            placed = []
            for c, bw, bh in blocks:
                if bw > size:
                    ok = False
                    break
                if x + bw > size:
                    x, y, row_h = 0, y + row_h, 0
                if y + bh > size:
                    ok = False
                    break
                placed.append((c, x, y))
                x += bw
                row_h = max(row_h, bh)
            if ok:
                for c, u, v in placed:
                    c.uv = (u, v)
                self.tex_w = self.tex_h = size
                return
        raise ValueError("model too large for a 2048 atlas")

    def _face_rects(self, c: Cube):
        w, h, d = self._dims(c)
        u, v = c.uv
        return {
            "up": (u + d, v, w, d),
            "down": (u + d + w, v, w, d),
            "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h),
        }

    # -- output ------------------------------------------------------------------
    def write(self, geo_path: Path, texture_path: Path, seed=1):
        self._pack()
        rng = random.Random(seed)
        tex = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        tp, gp = tex.load(), glow.load()
        any_glow = False
        bones_json = []
        for b in self.bones.values():
            bj = {"name": b.name, "pivot": list(_r(b.pivot))}
            if b.parent:
                bj["parent"] = b.parent
            if any(b.rotation):
                bj["rotation"] = list(_r(b.rotation))
            cubes_json = []
            for c in b.cubes:
                rects = self._face_rects(c)
                vertical = c.size[1] >= max(c.size[0], c.size[2])
                faces = {}
                for f, r in rects.items():
                    any_glow |= paint_face(tp, gp, r, c.material, f, rng, vertical, self.density)
                    uv_u, uv_v, uw, uh = r
                    if f == "down":  # Bedrock samples the bottom face flipped
                        faces[f] = {"uv": [uv_u, uv_v + uh], "uv_size": [uw, -uh]}
                    else:
                        faces[f] = {"uv": [uv_u, uv_v], "uv_size": [uw, uh]}
                cj = {"origin": list(_r(c.origin)), "size": list(_r(c.size)), "uv": faces}
                if c.inflate:
                    cj["inflate"] = round(c.inflate, 4)
                if c.rotation and any(c.rotation):
                    cj["pivot"] = list(_r(c.pivot or c.origin))
                    cj["rotation"] = list(_r(c.rotation))
                cubes_json.append(cj)
            if cubes_json:
                bj["cubes"] = cubes_json
            bones_json.append(bj)
        vw, vh, vo = self.visible_bounds
        geo = {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": self.identifier,
                    "texture_width": self.tex_w,
                    "texture_height": self.tex_h,
                    "visible_bounds_width": vw,
                    "visible_bounds_height": vh,
                    "visible_bounds_offset": list(vo),
                },
                "bones": bones_json,
            }],
        }
        geo_path.parent.mkdir(parents=True, exist_ok=True)
        texture_path.parent.mkdir(parents=True, exist_ok=True)
        geo_path.write_text(json.dumps(geo, indent=1) + "\n")
        tex.save(texture_path)
        self._tex = tex
        if any_glow:
            glow.save(texture_path.with_name(texture_path.stem + "_glowmask.png"))
        return self.stats()

    # -- preview -----------------------------------------------------------------
    def _bone_chain(self, name):
        chain = []
        while name:
            b = self.bones[name]
            chain.append(b)
            name = b.parent
        return chain  # leaf first

    def world_polys(self, pose=None):
        """Yields (material, face, [4 points]) in GeckoLib's in-game space, matching its
        BakedModelFactory/RenderUtil exactly: x is mirrored, x/y rotations are negated, and
        each rotation applies X first, then Y, then Z. Pose angles use the file convention."""
        pose = pose or {}
        for b in self.bones.values():
            chain = self._bone_chain(b.name)
            for c in b.cubes:
                inf = c.inflate
                x0, y0, z0 = (o - inf for o in c.origin)
                x1, y1, z1 = (c.origin[i] + c.size[i] + inf for i in range(3))
                corners = {
                    "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
                    "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
                    "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
                    "east": [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
                    "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                    "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
                }
                rects = self._face_rects(c) if c.uv else None
                for f, pts in corners.items():
                    out = []
                    for p in pts:
                        p = _mirror(p)
                        if c.rotation:
                            p = _rot(p, _mirror(c.pivot or c.origin), c.rotation)
                        for bb in chain:
                            r = tuple(bb.rotation[i] + pose.get(bb.name, (0, 0, 0))[i] for i in range(3))
                            if any(r):
                                p = _rot(p, _mirror(bb.pivot), r)
                        out.append(p)
                    yield c.material, f, out, (rects[f] if rects else None)

    def preview(self, path: Path, views=((25, 35), (0, 0), (0, 90), (0, 180)), size=520, pose=None, max_sub=10):
        """Software-rendered turnaround using the painted atlas (call after write())."""
        polys = list(self.world_polys(pose))
        tex = getattr(self, "_tex", None)
        tpx = tex.load() if tex else None
        xs = [p[0] for _, _, ps, _ in polys for p in ps]
        ys = [p[1] for _, _, ps, _ in polys for p in ps]
        zs = [p[2] for _, _, ps, _ in polys for p in ps]
        cx, cy, cz = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, (min(zs) + max(zs)) / 2
        ext = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs)) * 0.58 + 1
        img = Image.new("RGB", (size * len(views), size), (30, 32, 44))
        draw = ImageDraw.Draw(img, "RGBA")
        for vi, (pitch, yaw) in enumerate(views):
            cyaw, syaw = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
            cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

            def project(pt):
                x, y, z = pt[0] - cx, pt[1] - cy, pt[2] - cz
                x, z = x * cyaw - z * syaw, x * syaw + z * cyaw
                y, z = y * cp - z * sp, y * sp + z * cp
                return (vi * size + size / 2 - x / ext * size / 2, size / 2 - y / ext * size / 2, z)

            quads = []
            for mat, f, pts, rect in polys:
                q = [project(p) for p in pts]
                # back-face cull using screen-space winding is unreliable for inflated/rotated
                # boxes, so we rely on depth sorting of small sub-quads instead
                if tpx is None or rect is None:
                    quads.append((sum(p[2] for p in q) / 4, q, (*_shade(mat.color, FACE_LIGHT[f]), mat.alpha)))
                    continue
                u0, v0, w, h = rect
                nu, nv = max(1, min(w, max_sub)), max(1, min(h, max_sub))
                p00, p10, p11, p01 = q  # (min,min) (max,min) (max,max) (min,max) in face-local axes
                for a in range(nu):
                    for b in range(nv):
                        def lerp(sa, sb):
                            top = [p01[k] + (p11[k] - p01[k]) * sa for k in range(3)]
                            bot = [p00[k] + (p10[k] - p00[k]) * sa for k in range(3)]
                            return tuple(bot[k] + (top[k] - bot[k]) * sb for k in range(3))
                        sub = [lerp(a / nu, b / nv), lerp((a + 1) / nu, b / nv), lerp((a + 1) / nu, (b + 1) / nv), lerp(a / nu, (b + 1) / nv)]
                        tu = int(u0 + (a + 0.5) / nu * w)
                        tv = int(v0 + (1 - (b + 0.5) / nv) * h)
                        r, g, bl, al = tpx[min(tu, tex.width - 1), min(tv, tex.height - 1)]
                        if al == 0:
                            continue
                        quads.append((sum(p[2] for p in sub) / 4, sub, (r, g, bl, al)))
            quads.sort(key=lambda t: -t[0])
            for _, q, col in quads:
                draw.polygon([(p[0], p[1]) for p in q], fill=col)
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)


def _r(t):
    return tuple(round(x, 4) for x in t)


def _mirror(p):
    return (-p[0], p[1], p[2])


def _rot(p, pivot, rot):
    """Rotate a game-space point about pivot by file-convention euler degrees, the way
    GeckoLib does: angles become (-x, -y, z) and the matrix is Rz*Ry*Rx (X applied first)."""
    x, y, z = (p[i] - pivot[i] for i in range(3))
    rx, ry, rz = math.radians(-rot[0]), math.radians(-rot[1]), math.radians(rot[2])
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + pivot[0], y + pivot[1], z + pivot[2])


def write_animations(path: Path, animations: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"format_version": "1.8.0", "animations": animations}, indent=1) + "\n")
