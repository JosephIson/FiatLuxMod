"""The Archangel - second choir of the third sphere (Tier 2).

"For the Lord himself shall descend from heaven with a shout, with the voice of the archangel,
and with the trump of God" (1 Thessalonians 4:16). "Michael the archangel, when contending with
the devil" (Jude 1:9); "Michael, the great prince which standeth for the children of thy people"
(Daniel 12:1); "Michael and his angels fought against the dragon" (Revelation 12:7). Drawn as the
man in linen of Daniel 10 as captain of the Host: a breastplate of gold over linen, a crimson
cloak, great wings, the spear with which the dragon is thrust down, and the trumpet of God.
"""
import math

from geo import Model
from materials import *
from parts import limb
from figure import item_bone, figure, back_wings, held_staff, base_animations, sin


def build():
    m = Model("geometry.fiatlux.archangel", visible_bounds=(4.5, 4.5, (0, 1.6, 0)))
    pts = figure(m, LINEN, ROBE_SHADE, UPHAZ_GOLD, BERYL, BERYL_SHADE, LIGHTNING, TORCH_EYE, HAIR,
                 limb_mat=BRONZE, limb_glow=BRONZE_GLOW, mantle=CRIMSON_CLOAK, mantle_len=25.5, hold={"r": 75})

    # breastplate and pauldrons of gold, a sash of crimson
    m.cube("torso", (-4.2, 22.0, -2.6), (8.4, 6.8, 5.2), GOLD, inflate=0.12)
    m.cube("torso", (-3.3, 22.4, -2.95), (6.6, 5.6, 0.3), GOLD_DARK)
    m.centered("torso", (0, 26.0, -3.15), (1.8, 1.8, 0.3), BERYL_GLOW, rotation=(0, 0, 45), pivot=(0, 26.0, -3.15))
    m.cube("torso", (-4.3, 21.5, -2.8), (8.6, 0.9, 5.6), CRIMSON_CLOAK, rotation=(0, 0, 12), pivot=(0, 22, 0))
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        for k in range(2):
            m.cube(f"arm_{s}", (lo(3.6, 7.6), 26.8 - k * 1.4, -1.9), (4.0, 1.7, 3.8), GOLD,
                   rotation=(0, 0, side * (-14 - k * 8)), pivot=(X(5.0), 28.0, 0))
        m.cube(f"forearm_{s}", (lo(3.9, 7.1), 18.2, -1.6), (3.2, 3.4, 3.2), GOLD, inflate=0.05)

    # a fillet of gold binding the hair
    m.ring("head", (0, 35.6, 0), 3.85, 14, (1.8, 0.5, 0.4), GOLD)

    # the spear that thrusts down the dragon: shaft, collar, leaf blade edged with fire
    gx, gy, gz = pts["grip_r"]
    sp = item_bone(m, "spear", pts, "r")
    held_staff(m, sp, pts["grip_r"], 44.0, 2.0, WOOD, thick=0.6, band=GOLD)
    m.centered(sp, (gx, 44.4, gz), (1.0, 0.8, 1.0), GOLD)
    for rot in (0, 90):
        m.centered(sp, (gx, 47.6, gz), (1.6, 5.6, 0.35), STEEL, rotation=(0, rot, 0), pivot=(gx, 47.6, gz))
        m.centered(sp, (gx, 47.6, gz), (0.4, 5.6, 0.45), HOLY_FIRE_WHITE, rotation=(0, rot, 0), pivot=(gx, 47.6, gz))
    m.centered(sp, (gx, 50.6, gz), (0.7, 0.7, 0.7), STEEL, rotation=(45, 45, 0), pivot=(gx, 50.6, gz))

    # the trumpet of God (1 Thess 4:16): mouthpiece, long tapering tube, flared bell, held at the side
    tx, ty, tz = pts["grip_l"]
    tr = m.bone("trumpet", pts["grip_l"], "hand_l")
    path = [(tx, ty + 1.6, tz - 0.4), (tx, ty - 2.0, tz - 0.6), (tx, ty - 6.0, tz - 0.9), (tx, ty - 9.5, tz - 1.3)]
    limb(m, tr, (tx, ty + 2.2, tz - 0.35), path[0], 0.35, GOLD_GLOW)
    for k in range(len(path) - 1):
        limb(m, tr, path[k], path[k + 1], 0.55 + k * 0.15, GOLD)
    bx, by, bz = path[-1]
    m.ring(tr, (bx, by - 0.4, bz), 0.9, 10, (0.6, 0.5, 0.3), GOLD)
    m.ring(tr, (bx, by - 1.0, bz), 1.4, 12, (0.8, 0.4, 0.3), GOLD_GLOW)
    m.centered(tr, (tx, ty - 3.0, tz - 0.7), (1.1, 0.4, 1.1), GOLD_GLOW)

    back_wings(m, FEATHER, FEATHER_SHADE, FEATHER_GOLD, scale=1.3, primaries=9)
    return m


def animations():
    extra = {}
    judge = {   # the trumpet is lifted to the lips and sounded
        "arm_l": {"rotation": {"0.0": [0, 0, 0], "0.25": [-165, -20, 20], "1.0": [-165, -20, 20], "1.3": [0, 0, 0]}},
        "forearm_l": {"rotation": {"0.0": [0, 0, 0], "0.25": [-40, 0, 0], "1.0": [-40, 0, 0], "1.3": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.25": [-25, 0, 0], "1.0": [-25, 0, 0], "1.3": [0, 0, 0]}},
        "wing_l_root": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, -20, 40], "1.0": [0, -20, 40], "1.3": [0, 0, 0]}},
        "wing_r_root": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 20, -40], "1.0": [0, 20, -40], "1.3": [0, 0, 0]}},
    }
    return base_animations("archangel", primaries=9, extra_idle=extra, extra_move=extra, judge=judge, judge_len=1.3)
