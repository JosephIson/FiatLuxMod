"""Reusable anatomy for the Heavenly Host: feathered wings and watching eyes."""
import math

from materials import *


def wing(m, prefix, side, parent, shoulder, rotation, scale=1.0, primaries=8,
         feather=FEATHER, shade=FEATHER_SHADE, accent=FEATHER_GOLD, eyes=None,
         fan=(10, 7), bend=(10, 14)):
    """A fully articulated feathered wing: root -> arm -> forearm -> hand, layered coverts and
    secondaries, and one bone per primary feather (so animations can fan and fold them).

    Built in a local frame where the wing extends along +x (mirrored by side) with feathers
    hanging along -y, then placed at `shoulder`. `rotation` orients the whole wing.
    `eyes` (optional material) scatters watching eyes over the coverts (Ezekiel 10:12).
    `fan` = (base, step) degrees the primaries spread; a small fan gives a closed, veiling wing.
    `bend` = (forearm, hand) flex in degrees along the leading edge.
    Returns the root bone name."""
    sx, sy, sz = shoulder
    k_ = scale

    def P(x, y, z):  # local -> model space
        return (sx + side * x * k_, sy + y * k_, sz + z * k_)

    def box(bone, x0, x1, y0, h, z0, d, mat, rot=None, piv=None, inflate=0.0):
        a, b = sorted((sx + side * x0 * k_, sx + side * x1 * k_))
        r = None
        if rot:
            r = (rot[0], rot[1] * side, rot[2] * side)
        return m.cube(bone, (a, sy + y0 * k_, sz + z0 * k_), (b - a, h * k_, d * k_), mat,
                      rotation=r, pivot=P(*piv) if piv else None, inflate=inflate)

    root = m.bone(f"{prefix}_root", P(0, 0, 0), parent, rotation=(rotation[0], rotation[1] * side, rotation[2] * side))
    m.centered(root, P(0.5, 0.2, 0.2), (2.2 * k_, 2.6 * k_, 1.6 * k_), accent, inflate=0.1)
    arm = m.bone(f"{prefix}_arm", P(1.1, 0.5, 0.4), root)
    box(arm, 1.1, 9.7, -0.4, 2.0, -0.1, 1.4, feather)
    box(arm, 1.1, 9.1, 0.9, 0.8, 0.0, 1.2, accent, inflate=0.05)
    fore = m.bone(f"{prefix}_fore", P(9.5, 0.6, 0.5), arm, rotation=(0, 0, side * bend[0]))
    box(fore, 9.5, 17.7, -0.1, 1.5, 0.1, 1.2, feather)
    hand = m.bone(f"{prefix}_hand", P(17.5, 0.6, 0.6), fore, rotation=(0, 0, side * bend[1]))
    box(hand, 17.5, 23.9, 0.0, 1.2, 0.2, 1.0, feather)
    m.centered(hand, P(24.3, 0.6, 0.7), (0.9 * k_,) * 3, accent, rotation=(45, 0, 45), pivot=P(24.3, 0.6, 0.7))

    cov = m.bone(f"{prefix}_coverts", P(1.5, 0.0, 0.9), arm)
    for i in range(7):
        x = 1.5 + i * 1.2
        L = 4.2 + (i % 2) * 0.6
        box(cov, x, x + 1.8, -L, L, 0.85, 0.5, accent if i % 2 else feather, rot=(0, 0, 4 + i * 2), piv=(x, 0, 1.0))
    for i in range(6):
        x = 2.1 + i * 1.3
        box(cov, x, x + 1.6, 0.6 - 2.6, 2.6, 0.45, 0.45, shade, rot=(0, 0, 6), piv=(x, 0.6, 0.6))
    if eyes is not None:
        for i in range(4):
            x = 2.6 + i * 2.1
            box(cov, x, x + 0.9, -2.4 - (i % 2), 0.7, 1.32, 0.15, eyes)

    sec = m.bone(f"{prefix}_secondaries", P(9.5, 0.0, 1.2), fore)
    for i in range(8):
        x = 9.5 + i * 1.05
        L = 10.5 + (i % 3) * 0.7
        box(sec, x, x + 1.9, -L, L, 1.15, 0.4, feather if i % 2 == 0 else shade, rot=(0, 0, 2 + i * 1.5), piv=(x, 0, 1.3))
    for i in range(7):
        x = 10.0 + i * 1.1
        box(sec, x, x + 1.7, 0.2 - 7.5, 7.5, 0.65, 0.35, accent, rot=(0, 0, 3 + i * 1.5), piv=(x, 0.2, 0.8))

    for i in range(primaries):
        x = 17.5 + i * 0.85
        L = 12.0 + i * 0.9
        fb = m.bone(f"{prefix}_primary_{i}", P(x, 0.4, 1.3), hand, rotation=(0, 0, side * (fan[0] + i * fan[1])))
        box(fb, x - 0.9, x + 0.9, 0.4 - L, L, 1.1, 0.4, feather)
        box(fb, x - 0.15, x + 0.15, 0.4 - L + 0.8, L - 1.2, 1.05, 0.5, shade)
        if i % 2 == 0:
            box(fb, x - 0.8, x + 0.8, 0.4 - L * 0.55, L * 0.5, 0.75, 0.3, accent)
    return root


def wing_bones(prefix, primaries=8):
    return [f"{prefix}_{n}" for n in ("arm", "fore", "hand")] + [f"{prefix}_primary_{i}" for i in range(primaries)]


def eye(m, name, parent, center, normal, size=1.0, iris=None, lids=True):
    """A watching eye: sclera, iris, pupil and (optionally) a boned upper lid that blinks.
    `normal` is one of '-z','+z','-x','+x','+y','-y' (which way the eye looks)."""
    iris = iris or EYE_IRIS
    cx, cy, cz = center
    s = size
    rot = {"-z": (0, 0, 0), "+z": (0, 180, 0), "-x": (0, 90, 0), "+x": (0, -90, 0),
           "+y": (90, 0, 0), "-y": (-90, 0, 0)}[normal]
    b = m.bone(name, center, parent, rotation=rot)
    m.cube(b, (cx - 0.9 * s, cy - 0.55 * s, cz - 0.15 * s), (1.8 * s, 1.1 * s, 0.3 * s), SCLERA)
    m.cube(b, (cx - 0.45 * s, cy - 0.45 * s, cz - 0.25 * s), (0.9 * s, 0.9 * s, 0.2 * s), iris)
    m.cube(b, (cx - 0.2 * s, cy - 0.2 * s, cz - 0.32 * s), (0.4 * s, 0.4 * s, 0.1 * s), PUPIL)
    if lids:
        lid = m.bone(f"{name}_lid", (cx, cy + 0.55 * s, cz), b)
        m.cube(lid, (cx - 0.95 * s, cy + 0.25 * s, cz - 0.35 * s), (1.9 * s, 0.35 * s, 0.4 * s), LID)
    return b


def orient(arm_dir, feather_dir):
    """Solves the wing-root euler angles (file convention, for the side=+1 wing) that point
    the wing's leading edge along `arm_dir` with its feathers falling along `feather_dir`.
    Directions are in model file space (front = -z). The side=-1 wing is mirrored by wing().

    GeckoLib renders with x mirrored and angles (-rx, -ry, rz) composed as Rz*Ry*Rx, so the
    target basis is built in that game space and decomposed accordingly."""
    def norm(v):
        l = math.sqrt(sum(c * c for c in v))
        return tuple(c / l for c in v)

    def cross(a, b):
        return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    mir = lambda v: (-v[0], v[1], v[2])
    A = norm(mir(arm_dir))
    F = mir(feather_dir)
    d = sum(F[i] * A[i] for i in range(3))
    F = norm(tuple(F[i] - d * A[i] for i in range(3)))
    N = cross(A, F)
    a0, f0 = (-1.0, 0.0, 0.0), (0.0, -1.0, 0.0)
    n0 = cross(a0, f0)
    # R = [A F N] * [a0 f0 n0]^T
    R = [[A[r] * a0[c] + F[r] * f0[c] + N[r] * n0[c] for c in range(3)] for r in range(3)]
    gy = math.asin(max(-1.0, min(1.0, -R[2][0])))
    gx = math.atan2(R[2][1], R[2][2])
    gz = math.atan2(R[1][0], R[0][0])
    return (round(-math.degrees(gx), 3), round(-math.degrees(gy), 3), round(math.degrees(gz), 3))


def hand(m, prefix, side, parent, wrist, skin, shade, nail=None, curl=12.0):
    """An anatomically oriented relaxed hand for an arm hanging at the side.

    The palm faces the body (toward -side*x), the thumb sits on the front (-z) edge of the
    palm angled forward, and four three-segment fingers (index at the front, little finger at
    the back) hang from the knuckles and curl toward the palm. Nails sit on the back of the
    fingertips. Returns the hand bone; fingers are `<prefix>_finger_<0..3>` (+ `_mid`, `_tip`)
    and `<prefix>_thumb` (+ `_tip`)."""
    nail = nail or shade
    wx, wy, wz = wrist
    o = side            # +1: outward is +x; the palm faces -side*x
    def xs(a, b):       # x-range measured outward from the wrist centre
        x0, x1 = wx + o * a, wx + o * b
        return min(x0, x1), abs(x1 - x0)

    hb = m.bone(prefix, wrist, parent)
    # palm block, back-of-hand plate, heel of the palm and knuckle ridge
    x0, w = xs(-0.75, 0.75)
    m.cube(hb, (x0, wy - 2.6, wz - 1.3), (w, 2.6, 2.6), skin)
    x0, w = xs(0.45, 0.9)
    m.cube(hb, (x0, wy - 2.3, wz - 1.15), (w, 2.0, 2.3), shade)
    x0, w = xs(-0.95, -0.55)
    m.cube(hb, (x0, wy - 1.4, wz - 1.1), (w, 1.2, 1.2), skin)                       # thenar pad
    x0, w = xs(0.35, 0.95)
    m.cube(hb, (x0, wy - 2.8, wz - 1.25), (w, 0.45, 2.5), shade)                    # knuckle ridge

    lengths = (2.6, 2.9, 2.7, 2.1)
    zs = (-0.95, -0.32, 0.31, 0.92)
    for k, (L, fz) in enumerate(zip(lengths, zs)):
        top = wy - 2.6
        seg = (L * 0.45, L * 0.33, L * 0.22)
        th = 0.55 if k < 3 else 0.5
        name = f"{prefix}_finger_{k}"
        # curl toward the palm: rotate about z so the fingertip moves toward the body (sign accounts for GeckoLib x-mirroring)
        fb = m.bone(name, (wx, top, wz + fz), hb, rotation=(0, 0, o * curl * 0.4))
        x0, w = xs(-0.32, 0.32)
        m.cube(fb, (x0, top - seg[0], wz + fz - th / 2), (w, seg[0], th), skin)
        mid = m.bone(f"{name}_mid", (wx, top - seg[0], wz + fz), fb, rotation=(0, 0, o * curl))
        x0, w = xs(-0.3, 0.3)
        m.cube(mid, (x0, top - seg[0] - seg[1], wz + fz - th / 2 + 0.02), (w, seg[1], th - 0.04), skin)
        tip = m.bone(f"{name}_tip", (wx, top - seg[0] - seg[1], wz + fz), mid, rotation=(0, 0, o * curl))
        x0, w = xs(-0.28, 0.28)
        m.cube(tip, (x0, top - L, wz + fz - th / 2 + 0.04), (w, seg[2], th - 0.08), shade)
        x0, w = xs(0.22, 0.34)
        m.cube(tip, (x0, top - L + 0.1, wz + fz - th / 2 + 0.08), (w, seg[2] * 0.8, th - 0.16), nail)

    # thumb: rooted at the front-inner edge of the palm, angled forward and down
    tx = wx - o * 0.5
    tb = m.bone(f"{prefix}_thumb", (tx, wy - 0.9, wz - 1.2), hb, rotation=(-28, 0, o * 10))
    x0, w = min(tx - 0.33, tx + 0.33), 0.66
    m.cube(tb, (x0, wy - 2.3, wz - 1.55), (w, 1.45, 0.66), skin)
    tt = m.bone(f"{prefix}_thumb_tip", (tx, wy - 2.3, wz - 1.2), tb, rotation=(-12, 0, 0))
    m.cube(tt, (x0 + 0.03, wy - 3.3, wz - 1.52), (w - 0.06, 1.0, 0.6), shade)
    m.cube(tt, (x0 + 0.1, wy - 3.2, wz - 1.62), (w - 0.2, 0.7, 0.12), nail)
    return hb


def head(m, prefix, parent, base, skin, face, shade, lips, eye_mat, iris=None, hair=None, eyelids=True):
    """A sculpted humanoid head (7x7x7 envelope, front at -z) built from layered planes:
    cranium + occiput, forehead, brow ridge, recessed sockets, eyes (sclera/iris/pupil or a
    single emissive `eye_mat` when iris is None), boned upper eyelids, cheekbones, nose bridge,
    tip and alae, hinged jaw with lips and chin, and ears with helix and lobe.
    `face` paints the frontal features (e.g. LIGHTNING for Dan 10:6); `skin` the rest.
    Returns the head bone; the jaw is `<prefix>_jaw`, lids `<prefix>_eyelid_l/r`."""
    bx, by, bz = base
    hb = m.bone(prefix, (bx, by, bz), parent)
    Y = lambda y: by + y
    # cranium and occiput
    m.cube(hb, (bx - 3.5, Y(1.2), bz - 3.2), (7, 5.8, 6.7), skin)
    m.cube(hb, (bx - 3.2, Y(6.6), bz - 2.9), (6.4, 0.7, 6.0), skin)
    m.cube(hb, (bx - 3.0, Y(1.8), bz + 3.4), (6.0, 4.4, 0.5), skin)
    m.cube(hb, (bx - 3.75, Y(2.2), bz - 2.6), (0.35, 3.8, 5.0), skin)            # temples
    m.cube(hb, (bx + 3.4, Y(2.2), bz - 2.6), (0.35, 3.8, 5.0), skin)
    # forehead and brow ridge
    m.cube(hb, (bx - 3.2, Y(4.7), bz - 3.55), (6.4, 1.9, 0.4), face)
    m.cube(hb, (bx - 3.1, Y(4.2), bz - 3.85), (2.9, 0.5, 0.5), face, rotation=(0, 0, 4), pivot=(bx - 1.6, Y(4.45), bz - 3.6))
    m.cube(hb, (bx + 0.2, Y(4.2), bz - 3.85), (2.9, 0.5, 0.5), face, rotation=(0, 0, -4), pivot=(bx + 1.6, Y(4.45), bz - 3.6))
    m.cube(hb, (bx - 0.45, Y(4.0), bz - 3.75), (0.9, 0.7, 0.4), face)            # glabella
    for side, s in ((1, "l"), (-1, "r")):
        ex = bx + side * 1.55
        m.cube(hb, (ex - 1.05, Y(2.85), bz - 3.45), (2.1, 1.4, 0.2), shade)       # socket
        if iris is None:
            m.cube(hb, (ex - 0.8, Y(3.05), bz - 3.62), (1.6, 0.95, 0.22), eye_mat)
        else:
            m.cube(hb, (ex - 0.8, Y(3.05), bz - 3.6), (1.6, 0.95, 0.2), eye_mat)
            m.cube(hb, (ex - 0.38, Y(3.12), bz - 3.68), (0.76, 0.8, 0.12), iris)
            m.cube(hb, (ex - 0.16, Y(3.32), bz - 3.74), (0.32, 0.4, 0.08), PUPIL)
        if eyelids:
            lid = m.bone(f"{prefix}_eyelid_{s}", (ex, Y(4.05), bz - 3.7), hb)
            m.cube(lid, (ex - 0.88, Y(3.7), bz - 3.84), (1.76, 0.38, 0.3), skin)
        m.cube(hb, (ex - 0.85, Y(2.7), bz - 3.62), (1.7, 0.22, 0.25), skin)       # lower lid
        cx = bx + side * 2.25
        m.cube(hb, (cx - 0.8, Y(1.9), bz - 3.56), (1.6, 1.2, 0.3), face, rotation=(0, side * -14, 0), pivot=(cx, Y(2.5), bz - 3.4))  # cheekbone
        ear = bx + side * 3.6
        m.cube(hb, (ear - 0.25, Y(2.4), bz - 0.7), (0.5, 2.1, 1.5), skin, rotation=(0, side * -12, 0), pivot=(ear, Y(3.4), bz))     # helix
        m.cube(hb, (ear - 0.2, Y(2.0), bz - 0.4), (0.4, 0.6, 0.8), shade, rotation=(0, side * -12, 0), pivot=(ear, Y(3.4), bz))      # lobe
    # nose: bridge, tip, alae
    m.cube(hb, (bx - 0.38, Y(2.55), bz - 4.0), (0.76, 1.75, 0.55), face, rotation=(-8, 0, 0), pivot=(bx, Y(4.2), bz - 3.7))
    m.cube(hb, (bx - 0.55, Y(2.05), bz - 4.3), (1.1, 0.8, 0.75), face)
    for side in (1, -1):
        m.cube(hb, (bx + side * 0.62 - 0.22, Y(2.05), bz - 4.05), (0.44, 0.55, 0.5), shade)
    m.cube(hb, (bx - 0.2, Y(1.75), bz - 3.9), (0.4, 0.35, 0.25), shade)          # philtrum
    # hinged jaw: lips, chin, jawline
    jaw = m.bone(f"{prefix}_jaw", (bx, Y(1.6), bz - 0.6), hb)
    m.cube(jaw, (bx - 3.0, Y(0.2), bz - 3.25), (6.0, 1.3, 5.6), skin)
    m.cube(jaw, (bx - 0.95, Y(1.5), bz - 3.82), (1.9, 0.24, 0.3), lips)
    m.cube(jaw, (bx - 0.8, Y(1.18), bz - 3.8), (1.6, 0.3, 0.3), lips)
    m.cube(jaw, (bx - 1.25, Y(0.15), bz - 3.75), (2.5, 0.95, 0.6), face)          # chin
    for side in (1, -1):
        m.cube(jaw, (bx + side * 2.55 - 0.55, Y(0.3), bz - 2.9), (1.1, 1.2, 1.6), skin, rotation=(0, side * 20, 0), pivot=(bx + side * 2.55, Y(0.9), bz - 2.1))
    if hair is not None:
        m.cube(hb, (bx - 3.8, Y(5.9), bz - 3.8), (7.6, 1.5, 7.6), hair)
        m.cube(hb, (bx - 3.8, Y(3.1), bz + 1.2), (7.6, 3.6, 2.7), hair)
        m.cube(hb, (bx - 3.6, Y(5.3), bz - 3.75), (7.2, 0.7, 1.0), hair)          # hairline fringe
    return hb


def veil_wing(m, prefix, side, parent, root, wrist, bar_y, front_z, inner_x, outer_x, length,
              feathers=7, feather=FEATHER, shade=FEATHER_SHADE, accent=FEATHER_GOLD, wrap=28.0, eyes=None):
    """A wing folded forward as a veil (Isaiah 6:2: "with twain he covered his face, and with
    twain he covered his feet"). The wing arm rises from `root` (on the back) and arches to
    `wrist` in front of the body; from a carpal bar at height `bar_y` and depth `front_z` a
    curtain of overlapping flight feathers hangs straight down for `length`, spanning x from
    `outer_x` to `inner_x` (both given for side=+1; mirrored otherwise). Two layers - long
    flight feathers behind, coverts in front - each feather on its own bone so it can
    tremble; the outermost feathers wrap backwards around the side by up to `wrap` degrees."""
    X = lambda x: side * x
    rx, ry, rz = root
    wx, wy, wz = wrist
    rb = m.bone(f"{prefix}_root", (X(rx), ry, rz), parent)
    m.centered(rb, (X(rx), ry, rz), (2.0, 2.4, 1.8), accent, inflate=0.1)

    # arm: two segments arching from the back over the shoulder to the wrist
    mid = ((rx + wx) / 2, max(ry, wy) + 2.0, (rz + wz) / 2)
    def seg(bone, a, b, thick, mat):
        ax, ay, az = a
        bx, by, bz = b
        dx, dy, dz = bx - ax, by - ay, bz - az
        L = math.sqrt(dx * dx + dy * dy + dz * dz)
        c = ((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2)
        # orient a box whose long axis is y: pitch toward z, then roll toward x
        pitch = math.degrees(math.atan2(dz, dy))
        roll = -math.degrees(math.atan2(dx, math.sqrt(dy * dy + dz * dz)))
        m.cube(bone, (c[0] - thick / 2, c[1] - L / 2, c[2] - thick / 2), (thick, L, thick), mat,
               rotation=(-pitch, 0, roll), pivot=c)
    arm = m.bone(f"{prefix}_arm", (X(rx), ry, rz), rb)
    seg(arm, (X(rx), ry, rz), (X(mid[0]), mid[1], mid[2]), 1.5, feather)
    seg(arm, (X(rx), ry + 0.5, rz), (X(mid[0]), mid[1] + 0.5, mid[2]), 0.9, accent)
    fore = m.bone(f"{prefix}_fore", (X(mid[0]), mid[1], mid[2]), arm)
    seg(fore, (X(mid[0]), mid[1], mid[2]), (X(wx), wy, wz), 1.3, feather)
    hand = m.bone(f"{prefix}_hand", (X(wx), wy, wz), fore)
    lo, hi = sorted((X(inner_x), X(outer_x)))
    m.cube(hand, (lo, bar_y - 0.6, front_z - 0.7), (hi - lo, 1.4, 1.3), feather)          # carpal bar
    m.cube(hand, (lo, bar_y + 0.4, front_z - 0.8), (hi - lo, 0.6, 1.0), accent, inflate=0.05)

    span = abs(outer_x - inner_x)
    w = span / feathers * 1.35                     # overlap between neighbours
    for k in range(feathers):
        t = k / max(1, feathers - 1)               # 0 = inner edge, 1 = outer edge
        x = inner_x + (outer_x - inner_x) * t
        L = length * (0.96 + 0.08 * math.sin(k * 1.7))
        yaw = side * wrap * max(0.0, t - 0.55) / 0.45   # outer feathers wrap around the side
        fb = m.bone(f"{prefix}_feather_{k}", (X(x), bar_y, front_z), hand, rotation=(0, yaw, 0))
        lo = min(X(x - w / 2), X(x + w / 2))
        m.cube(fb, (lo, bar_y - L, front_z + 0.15), (w, L, 0.35), feather if k % 2 == 0 else shade)
        m.cube(fb, (X(x) - 0.12, bar_y - L + 0.6, front_z + 0.05), (0.24, L - 1.0, 0.2), accent)   # rachis
        cl = L * 0.45                                                                         # covert
        m.cube(fb, (lo + 0.1, bar_y - cl, front_z - 0.35), (w - 0.2, cl, 0.35), accent if k % 2 else feather)
        if eyes is not None and k % 2 == 0:   # Ezek 10:12 - wings full of eyes
            m.cube(fb, (X(x) - 0.5, bar_y - cl * 0.6, front_z - 0.45), (1.0, 0.75, 0.1), eyes)
            m.cube(fb, (X(x) - 0.45, bar_y - L * 0.75, front_z + 0.05 - 0.1), (0.9, 0.7, 0.1), eyes)
    return rb


def limb(m, bone, a, b, thick, mat, inflate=0.0):
    """A square-section box spanning point a to point b (file space), oriented with GeckoLib's
    conventions. Chain several to draw horns, tusks, arches and tails without gaps."""
    ax, ay, az = a
    bx, by, bz = b
    dx, dy, dz = bx - ax, by - ay, bz - az
    L = math.sqrt(dx * dx + dy * dy + dz * dz)
    c = ((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2)
    # rotate the +y axis onto d: tilt toward z about x, then lean toward x about z
    pitch = math.degrees(math.atan2(dz, dy))
    roll = math.degrees(math.atan2(dx, math.sqrt(dy * dy + dz * dz)))
    return m.cube(bone, (c[0] - thick / 2, c[1] - L / 2 - thick * 0.15, c[2] - thick / 2), (thick, L + thick * 0.3, thick), mat,
                  rotation=(-pitch, 0, roll), pivot=c, inflate=inflate)


def curve(m, bone, points, thick0, thick1, mat):
    """Tapered chain of limb() segments through `points`."""
    n = len(points) - 1
    for i in range(n):
        t = thick0 + (thick1 - thick0) * (i / max(1, n - 1))
        limb(m, bone, points[i], points[i + 1], t, mat)
