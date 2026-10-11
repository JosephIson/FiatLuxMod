"""The Serpent of Eden (Tier 2).

Genesis 3:1: "Now the serpent was more subtil than any beast of the field which the LORD God had
made." Revelation 12:9 names him: "that old serpent, called the Devil, and Satan."

Beautiful, not monstrous: a long body of iridescent green-gold scales over a pale belly, a slender
neck raised in an S, a crested head with eyes of gold and a forked tongue. Before the curse he
stands on four small clawed legs; the curse takes them ("upon thy belly shalt thou go", 3:14) - the
renderer hides the leg bones once he is cursed. Fourteen chained body bones let him slither.
"""
import math

from geo import Material, Model
from parts import limb
from figure import sin

SCALES = Material("sp_scales", (52, 112, 66), "scale", accent=(214, 186, 70))
SCALES_DARK = Material("sp_scales_dark", (34, 76, 48), "scale", accent=(120, 170, 90))
BELLY = Material("sp_belly", (226, 214, 168), "skin")
CREST = Material("sp_crest", (200, 150, 50), "gold", accent=(255, 230, 140))
EYE = Material("sp_eye", (255, 196, 40), "plain", emissive=True)
PUPIL = Material("sp_pupil", (20, 14, 10), "plain")
TONGUE = Material("sp_tongue", (190, 40, 60), "skin")
CLAW = Material("sp_claw", (60, 50, 40), "plain")

SEG = 3.6
N = 14


def width(k):
    return 4.4 * (1 - k / (N + 2)) + 0.9


def build():
    m = Model("geometry.fiatlux.serpent", visible_bounds=(4, 2, (0, 0.6, 0)))
    m.bone("root", (0, 0, 0))
    parent = "root"
    # the body, lying back from the neck along +z in a chain of bones
    for k in range(N):
        z0 = 2.0 + k * SEG
        w = width(k)
        hgt = w * 0.78
        b = m.bone(f"body_{k}", (0, hgt / 2, z0), parent)
        m.cube(b, (-w / 2, 0, z0 - 0.3), (w, hgt, SEG + 0.6), SCALES if k % 3 else SCALES_DARK)
        m.cube(b, (-w / 2 + 0.3, -0.05, z0 - 0.2), (w - 0.6, 0.35, SEG + 0.4), BELLY)
        m.cube(b, (-0.5, hgt - 0.1, z0), (1.0, 0.35, SEG), CREST if k < 4 else SCALES_DARK)       # dorsal ridge
        for side in (1, -1):   # a band of darker flank scales, the pattern of a diamond-back
            m.cube(b, (side * w / 2 - (0.25 if side > 0 else -0.05), hgt * 0.35, z0 + 0.4), (0.2, hgt * 0.35, SEG - 0.8), SCALES_DARK)
        parent = b
    tail = m.bone("tail_tip", (0, 0.5, 2.0 + N * SEG), parent)
    limb(m, tail, (0, 0.5, 2.0 + N * SEG), (0, 0.4, 2.0 + N * SEG + 4.0), 0.7, SCALES)
    # four small clawed legs, before the curse
    for side, s in ((1, "l"), (-1, "r")):
        for fb, z in (("f", 3.5), ("b", 10.5)):
            leg = m.bone(f"leg_{fb}{s}", (side * 2.0, 1.6, z), "body_0" if fb == "f" else "body_2")
            hip, knee, foot = (side * 2.0, 1.6, z), (side * 3.6, 1.8, z - 0.4), (side * 4.2, 0.0, z - 0.8)
            limb(m, leg, hip, knee, 1.0, SCALES)
            limb(m, leg, knee, foot, 0.8, SCALES)
            for c in (-1, 0, 1):
                limb(m, leg, foot, (foot[0] + side * 0.6, 0.0, foot[2] - 0.9 + c * 0.5 * side * 0), 0.3, CLAW)
                m.cube(leg, (foot[0] - 0.15 + c * 0.35, 0.0, foot[2] - 1.0), (0.3, 0.25, 0.8), CLAW)
    # the neck, raised in an S, in four chained bones
    pts = [(0, 1.6, 2.4), (0, 5.0, 1.0), (0, 9.0, 0.4), (0, 12.6, 1.2), (0, 14.6, 0.2)]
    parent = "root"
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        nb = m.bone("neck" if i == 0 else f"neck_{i}", a, parent)
        limb(m, nb, a, b, 3.6 - i * 0.35, SCALES)
        limb(m, nb, (a[0], a[1], a[2] - 1.5), (b[0], b[1], b[2] - 1.4), 1.6 - i * 0.12, BELLY)
        for side in (1, -1):
            limb(m, nb, (side * 1.5, a[1] + 0.4, a[2]), (side * 1.4, b[1] - 0.2, b[2]), 0.5, SCALES_DARK)
        parent = nb
    neck = parent
    # the head: a wedge of a skull, crest, gold eyes, hinged jaw and forked tongue
    hx, hy, hz = 0, 15.0, -0.4
    head = m.bone("head", (hx, hy, hz), neck)
    m.cube(head, (-2.0, hy - 0.6, hz - 5.6), (4.0, 2.4, 6.2), SCALES)
    m.cube(head, (-1.5, hy - 0.4, hz - 7.6), (3.0, 1.8, 2.2), SCALES)            # snout
    m.cube(head, (-2.1, hy + 1.6, hz - 4.6), (4.2, 0.5, 4.6), SCALES_DARK)        # brow plate
    for side in (1, -1):
        ex = side * 1.9
        m.cube(head, (ex - 0.3 if side > 0 else ex - 0.2, hy + 0.6, hz - 4.4), (0.5, 0.9, 1.3), EYE)
        m.cube(head, (ex - 0.25 if side > 0 else ex - 0.15, hy + 0.7, hz - 3.9), (0.55, 0.7, 0.25), PUPIL)
        m.cube(head, (side * 1.2 - 0.2, hy + 0.9, hz - 7.7), (0.4, 0.3, 0.3), PUPIL)   # nostrils
        lid = m.bone(f"eyelid_{'l' if side > 0 else 'r'}", (ex, hy + 1.5, hz - 3.8), head)
        m.cube(lid, (ex - 0.35 if side > 0 else ex - 0.25, hy + 1.3, hz - 4.5), (0.6, 0.3, 1.5), SCALES_DARK)
    crest = m.bone("crest", (0, hy + 2.0, hz - 1.0), head)
    for i, (dz, h, tilt) in enumerate(((-3.2, 2.2, 30), (-1.6, 3.0, 40), (0.0, 3.4, 48), (1.6, 2.8, 55), (3.0, 2.0, 60))):
        for side in (1, -1):
            x = side * (0.9 + i * 0.25)
            limb(m, crest, (x, hy + 1.8, hz + dz - 1.0), (x * 1.8, hy + 1.8 + h, hz + dz + 1.5), 0.45, CREST)
    jaw = m.bone("jaw", (0, hy - 0.5, hz - 0.4), head)
    m.cube(jaw, (-1.8, hy - 1.4, hz - 5.4), (3.6, 0.9, 5.4), BELLY)
    m.cube(jaw, (-1.3, hy - 1.3, hz - 7.3), (2.6, 0.8, 2.0), BELLY)
    tongue = m.bone("tongue", (0, hy - 0.5, hz - 7.4), head)
    limb(m, tongue, (0, hy - 0.5, hz - 6.8), (0, hy - 0.5, hz - 9.4), 0.3, TONGUE)
    for side in (1, -1):
        limb(m, tongue, (0, hy - 0.5, hz - 9.4), (side * 0.5, hy - 0.5, hz - 10.6), 0.25, TONGUE)
    return m


def animations():
    idle = {
        "neck": {"rotation": [sin(60, 0, 3), sin(40, 0, 10), 0]},
        "head": {"rotation": [sin(50, 30, 6), sin(35, 60, 12), 0]},
        "tongue": {"scale": [1, 1, "math.sin(query.anim_time * 160) > 0.9 ? 1 : 0.15"]},
    }
    for k in range(N):
        idle[f"body_{k}"] = {"rotation": [0, sin(45, k * 25, 4), 0]}
    for s_ in ("l", "r"):   # a slow blink
        idle[f"eyelid_{s_}"] = {"position": {"0.0": [0, 0, 0], "5.8": [0, 0, 0], "5.9": [0, -0.9, 0], "6.0": [0, 0, 0]}}
    move = dict(idle)
    for k in range(N):
        move[f"body_{k}"] = {"rotation": [0, sin(260, k * 40, 16), 0]}
    move["neck"] = {"rotation": [sin(130, 0, 4), sin(130, 30, 8), 0]}
    for i in (1, 2, 3):
        idle[f"neck_{i}"] = {"rotation": [sin(60, i * 40, 2), sin(40, i * 50, 4), 0]}
    speak = dict(idle)
    speak["head"] = {"rotation": [f"-8 + {sin(240, 0, 6)}", sin(70, 0, 14), 0]}
    speak["jaw"] = {"rotation": [f"math.max(0, {sin(700, 0, 14)})", 0, 0]}
    return {
        "animation.serpent.idle": {"loop": True, "animation_length": 8.0, "bones": idle},
        "animation.serpent.move": {"loop": True, "animation_length": 1.38, "bones": move},
        "animation.serpent.speak": {"loop": True, "animation_length": 3.0, "bones": speak},
    }
