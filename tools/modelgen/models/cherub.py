"""Cherub - one of the four living creatures beneath the throne (Tier 3).

Ezekiel 1:5-13 (KJV): "they had the likeness of a man. And every one had four faces, and every
one had four wings. And their feet were straight feet; and the sole of their feet was like the
sole of a calf's foot: and they sparkled like the colour of burnished brass. And they had the
hands of a man under their wings on their four sides ... As for the likeness of their faces,
they four had the face of a man, and the face of a lion, on the right side: and they four had
the face of an ox on the left side; they four also had the face of an eagle ... their wings were
stretched upward; two wings of every one were joined one to another, and two covered their
bodies ... their appearance was like burning coals of fire, and like the appearance of lamps."
Ezekiel 10:12: "their whole body, and their backs, and their hands, and their wings ... were
full of eyes round about."  (10:14 names "the face of a cherub" where 1:10 has the ox.)

Front (-z) is the man; the lion is on the creature's right (-x, the side of arm_r), the ox on
its left (+x), and the eagle behind (+z).
"""
import math

from geo import Model
from materials import *
from parts import curve, eye, hand, orient, veil_wing, wing


def build_head(m, parent, base=(0, 30.0, 0)):
    bx, by, bz = base
    Y = lambda y: by + y
    h = m.bone("head", (bx, by, bz), parent)
    m.cube(h, (bx - 4, Y(0.4), bz - 4), (8, 7.6, 8), CHERUB_FLESH)
    m.cube(h, (bx - 3.6, Y(7.9), bz - 3.6), (7.2, 0.6, 7.2), CHERUB_FLESH_SHADE)

    # --- the face of a man (front): Assyrian-style curled beard, as on the kerubim of the age
    m.cube(h, (bx - 3.4, Y(5.4), bz - 4.3), (6.8, 1.6, 0.4), CHERUB_FLESH)                 # forehead
    m.cube(h, (bx - 3.2, Y(5.0), bz - 4.55), (6.4, 0.5, 0.45), CHERUB_FLESH_SHADE)         # brow
    for side, s in ((1, "l"), (-1, "r")):
        eye(m, f"man_eye_{s}", h, (bx + side * 1.6, Y(4.3), bz - 4.1), "-z", size=0.9, iris=EYE_IRIS_GOLD)
    m.cube(h, (bx - 0.45, Y(2.9), bz - 4.7), (0.9, 2.0, 0.6), CHERUB_FLESH, rotation=(-8, 0, 0), pivot=(bx, Y(4.8), bz - 4.3))
    m.cube(h, (bx - 0.65, Y(2.5), bz - 5.0), (1.3, 0.8, 0.8), CHERUB_FLESH)
    m.cube(h, (bx - 1.5, Y(2.0), bz - 4.6), (3.0, 0.4, 0.35), BEARD)                        # moustache
    m.cube(h, (bx - 0.9, Y(1.5), bz - 4.5), (1.8, 0.35, 0.3), Material("lip", (170, 96, 80), "skin"))
    beard = m.bone("beard", (bx, Y(1.4), bz - 4.2), h)
    for k in range(6):  # six braided locks, three curls each
        x = bx - 3.0 + k * 1.2
        for c in range(3):
            y = Y(0.9) - c * 1.25
            m.cube(beard, (x - 0.55, y - 1.1, bz - 4.5 + c * 0.25), (1.1, 1.2, 1.0), BEARD,
                   rotation=(0, 0, 18 if (k + c) % 2 else -18), pivot=(x, y - 0.55, bz - 4.0))
    m.cube(beard, (bx - 3.4, Y(-3.0), bz - 4.0), (6.8, 3.6, 0.6), BEARD)

    # --- the face of a lion (right side, -x) framed by a mane
    m.cube(h, (bx - 6.6, Y(1.2), bz - 1.7), (2.7, 2.8, 3.4), LION)                         # muzzle
    m.cube(h, (bx - 6.9, Y(3.1), bz - 0.8), (0.6, 0.9, 1.6), LION_NOSE)                    # nose pad
    m.cube(h, (bx - 6.4, Y(0.6), bz - 1.3), (2.4, 0.8, 2.6), LION)                         # jaw
    m.cube(h, (bx - 6.75, Y(1.5), bz - 1.6), (0.3, 0.25, 3.2), LION_NOSE)                  # mouth line
    m.cube(h, (bx - 4.5, Y(4.4), bz - 3.0), (0.6, 2.4, 6.0), LION)                         # brow plate
    for side, s in ((1, "a"), (-1, "b")):
        eye(m, f"lion_eye_{s}", h, (bx - 4.1, Y(4.0), bz + side * 1.5), "-x", size=0.85, iris=EYE_IRIS_GOLD)
        m.cube(h, (bx - 4.6, Y(7.3), bz + side * 2.6 - 0.6), (1.0, 1.4, 1.2), LION_MANE)    # ears
    m.ring(h, (bx - 4.4, Y(3.6), bz), 4.3, 14, (1.4, 2.0, 1.6), LION_MANE, axis="x")
    m.ring(h, (bx - 4.0, Y(3.6), bz), 3.6, 10, (1.2, 1.6, 1.3), LION, axis="x", phase=0.22)

    # --- the face of an ox (left side, +x): broad muzzle, nostrils, curving horns
    m.cube(h, (bx + 3.9, Y(0.8), bz - 1.8), (3.0, 3.0, 3.6), OX)
    m.cube(h, (bx + 6.8, Y(1.0), bz - 1.5), (0.4, 1.8, 3.0), OX_NOSE)
    for side, s in ((1, "a"), (-1, "b")):
        m.cube(h, (bx + 7.0, Y(1.9), bz + side * 0.8 - 0.3), (0.2, 0.6, 0.6), PUPIL)       # nostrils
        eye(m, f"ox_eye_{s}", h, (bx + 4.1, Y(4.6), bz + side * 1.7), "+x", size=0.85, iris=Material("ox_iris", (110, 70, 40), "plain"))
        m.cube(h, (bx + 4.0, Y(5.4), bz + side * 3.9 - 0.7), (0.4, 1.0, 1.4), OX)          # ears
        p0 = (bx + 3.3, Y(7.3), bz + side * 2.3)
        horn = m.bone(f"horn_{s}", p0, h)
        pts = [p0,
               (p0[0] + 0.3, p0[1] + 0.3, p0[2] + side * 1.8),
               (p0[0] + 0.5, p0[1] + 1.4, p0[2] + side * 3.2),
               (p0[0] + 0.3, p0[1] + 3.0, p0[2] + side * 3.8),
               (p0[0] - 0.4, p0[1] + 4.3, p0[2] + side * 3.6)]
        curve(m, horn, pts, 1.3, 0.55, HORN)

    # --- the face of an eagle (back, +z): hooked beak, fierce brow, white head feathers
    m.cube(h, (bx - 1.0, Y(2.6), bz + 4.0), (2.0, 1.8, 1.8), BEAK)
    m.cube(h, (bx - 0.7, Y(1.2), bz + 5.4), (1.4, 1.9, 1.0), BEAK, rotation=(-28, 0, 0), pivot=(bx, Y(2.8), bz + 5.6))
    m.cube(h, (bx - 0.8, Y(1.8), bz + 4.0), (1.6, 0.6, 1.4), Material("beak_low", (220, 170, 50), "plain"))
    m.cube(h, (bx - 1.1, Y(4.1), bz + 4.0), (2.2, 0.6, 0.9), Material("cere", (250, 210, 90), "plain"))
    for side, s in ((1, "a"), (-1, "b")):
        eye(m, f"eagle_eye_{s}", h, (bx + side * 2.0, Y(4.6), bz + 4.1), "+z", size=0.8, iris=EYE_IRIS_GOLD)
        m.cube(h, (bx + side * 2.0 - 1.1, Y(5.3), bz + 4.0), (2.2, 0.6, 0.8), EAGLE, rotation=(0, 0, side * 12), pivot=(bx + side * 2.0, Y(5.6), bz + 4.4))
    crest = m.bone("eagle_crest", (bx, Y(8.0), bz + 4.0), h)
    for k in range(9):  # white head feathers above and around the eyes, swept back
        x = bx - 3.4 + (k % 5) * 1.7
        y = Y(8.2) - (k // 5) * 0.8
        m.cube(crest, (x - 0.8, y - 2.2, bz + 4.0), (1.6, 2.4, 0.5), EAGLE_WHITE, rotation=(-18, 0, (k - 4) * 3), pivot=(x, y, bz + 4.2))
    for side in (1, -1):  # cheek feathers framing the beak
        m.cube(h, (bx + side * 2.6 - 0.9, Y(1.0), bz + 4.0), (1.8, 2.6, 0.5), EAGLE_WHITE)
    return h


def _arm(m, name, side, parent, shoulder, length, rot):
    X = lambda x: side * x
    sx, sy, sz = shoulder
    a = m.bone(f"arm_{name}", shoulder, parent, rotation=rot)
    m.cube(a, (sx - 1.5, sy - length * 0.55, sz - 1.5), (3, length * 0.55, 3), CHERUB_COAL)
    m.centered(a, (sx, sy - 0.6, sz), (3.6, 2.2, 3.6), CHERUB_COAL, inflate=0.1)
    fy = sy - length * 0.55
    f = m.bone(f"forearm_{name}", (sx, fy, sz), a)
    m.cube(f, (sx - 1.35, fy - length * 0.45, sz - 1.35), (2.7, length * 0.45, 2.7), CHERUB_COAL)
    eye(m, f"forearm_eye_{name}", f, (sx + side * 1.4, fy - length * 0.22, sz), "+x" if side > 0 else "-x", size=0.6, lids=False)
    hand(m, f"hand_{name}", side, f, (sx, fy - length * 0.45, sz), CHERUB_FLESH, CHERUB_FLESH_SHADE)


def build():
    m = Model("geometry.fiatlux.cherub", visible_bounds=(7, 6, (0, 2.0, 0)))
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 18, 0), "root")

    # straight legs (no bending knee) ending in calf's hooves of burnished brass
    for side, s in ((1, "l"), (-1, "r")):
        lg = m.bone(f"leg_{s}", (side * 2.0, 18.0, 0), "body")
        x = side * 2.0
        m.cube(lg, (x - 1.6, 3.0, -1.6), (3.2, 15.5, 3.2), CHERUB_COAL)
        m.cube(lg, (x - 1.3, 2.0, -1.3), (2.6, 1.2, 2.6), CHERUB_COAL)                   # pastern
        for toe in (-1, 1):                                                              # cloven hoof
            m.cube(lg, (x + toe * 0.75 - 0.65, 0.0, -2.2), (1.3, 2.2, 2.8), HOOF, rotation=(0, toe * 6, 0), pivot=(x + toe * 0.75, 1, -0.8))
        m.cube(lg, (x - 0.4, 0.8, 1.2), (0.8, 0.7, 0.6), HOOF)                           # dewclaw
        eye(m, f"thigh_eye_{s}", lg, (x, 13.0, -1.65), "-z", size=0.6, lids=False)

    # torso with eyes before and behind (10:12)
    m.cube("body", (-4.0, 16.5, -2.4), (8, 5.5, 4.8), CHERUB_COAL)
    m.bone("torso", (0, 22.0, 0), "body")
    m.cube("torso", (-4.6, 21.8, -2.6), (9.2, 7.0, 5.2), CHERUB_COAL)
    m.cube("torso", (-4.0, 22.4, -2.9), (8.0, 5.2, 0.4), CHERUB_COAL, inflate=0.05)        # pectoral plane
    for k, (x, y) in enumerate(((-2.2, 26.6), (2.2, 26.6), (0.0, 24.0))):
        eye(m, f"chest_eye_{k}", "torso", (x, y, -3.0), "-z", size=0.8)
    for k, (x, y) in enumerate(((-2.4, 26.0), (2.4, 26.0), (0.0, 23.0))):
        eye(m, f"back_eye_{k}", "torso", (x, y, 2.7), "+z", size=0.8)
    m.bone("neck", (0, 28.8, 0), "torso")
    m.cube("neck", (-1.9, 28.6, -1.9), (3.8, 1.8, 3.8), CHERUB_FLESH)
    build_head(m, "neck")

    # the hands of a man under their wings on their four sides: two pairs of arms
    _arm(m, "upper_l", 1, "torso", (5.6, 28.4, 0), 11.5, (0, 0, -6))
    _arm(m, "upper_r", -1, "torso", (-5.6, 28.4, 0), 11.5, (0, 0, 6))
    _arm(m, "lower_l", 1, "torso", (4.2, 23.0, -2.2), 9.0, (-35, 0, -8))
    _arm(m, "lower_r", -1, "torso", (-4.2, 23.0, -2.2), 9.0, (-35, 0, 8))

    # four wings: two stretched upward, two covering the body; wings full of eyes
    for side, s in ((1, "l"), (-1, "r")):
        wing(m, f"wing_up_{s}", side, "torso", (2.6, 28.6, 2.8), orient((0.3, 1, 0.3), (1, -0.15, 0.35)),
             scale=1.1, primaries=8, feather=CHERUB_FEATHER, shade=CHERUB_FEATHER_SHADE,
             accent=CHERUB_FEATHER_GOLD, eyes=EYE_PAINTED, fan=(6, 5))
        veil_wing(m, f"wing_cover_{s}", side, "torso", root=(2.8, 27.6, 2.6), wrist=(4.8, 29.4, -4.6),
                  bar_y=29.2, front_z=-4.9, inner_x=-1.4, outer_x=5.2, length=13.5, feathers=7,
                  feather=CHERUB_FEATHER, shade=CHERUB_FEATHER_SHADE, accent=CHERUB_FEATHER_GOLD,
                  eyes=EYE_PAINTED)
    return m


def animations():
    sin = lambda speed, phase=0, amp=1: f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    idle = {
        "body": {"position": [0, sin(90, 0, 0.4), 0]},
        "beard": {"rotation": [sin(60, 0, 2), 0, 0]},
        "eagle_crest": {"rotation": [sin(80, 30, 3), 0, 0]},
    }
    # every eye blinks on its own rhythm - "full of eyes round about"
    blink_eyes = ["man_eye_l", "man_eye_r", "lion_eye_a", "lion_eye_b", "ox_eye_a", "ox_eye_b",
                  "eagle_eye_a", "eagle_eye_b"] + [f"chest_eye_{k}" for k in range(3)] + [f"back_eye_{k}" for k in range(3)]
    for k, e in enumerate(blink_eyes):
        t = round(0.4 + (k * 0.73) % 3.4, 2)
        idle[f"{e}_lid"] = {"scale": {"0.0": [1, 1, 1], str(t): [1, 1, 1], str(round(t + 0.08, 2)): [1, 3.2, 1], str(round(t + 0.16, 2)): [1, 1, 1], "4.0": [1, 1, 1]}}
    for s, sign in (("l", 1), ("r", -1)):
        idle[f"wing_up_{s}_arm"] = {"rotation": [0, 0, sin(90, 0, 4 * sign)]}
        for k in range(8):
            idle[f"wing_up_{s}_primary_{k}"] = {"rotation": [0, 0, sin(90, k * 20, 2 * sign)]}
        for k in range(7):
            idle[f"wing_cover_{s}_feather_{k}"] = {"rotation": [sin(240, k * 30, 1.2), 0, 0]}
    # "they went every one straight forward ... they turned not when they went" (1:9)
    move = dict(idle)
    move["body"] = {"position": [0, sin(240, 0, 0.6), 0]}
    for s, sign in (("l", 1), ("r", -1)):
        move[f"wing_up_{s}_arm"] = {"rotation": [0, 0, sin(240, 0, 10 * sign)]}
        # "when they went, I heard the noise of their wings, like the noise of great waters" (1:24)
        for k in range(8):
            move[f"wing_up_{s}_primary_{k}"] = {"rotation": [0, 0, sin(480, k * 25, 4 * sign)]}
    return {
        "animation.cherub.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.cherub.move": {"loop": True, "animation_length": 1.5, "bones": move},
    }
