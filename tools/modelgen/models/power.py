"""The Power (Exousiai) - third choir of the second sphere (Tier 3).

Ephesians 6:12 names the "principalities" and "powers" of darkness the faithful wrestle against;
Colossians 1:16 names the Powers among the holy ones created through Christ. Tradition makes the
holy Powers the warrior choir that holds the fallen in check - "the angels which kept not their
first estate, he hath reserved in everlasting chains" (Jude 1:6; cf. Revelation 20:1-2, "an angel
... having ... a great chain in his hand"). Drawn as the man in linen armed for war (Ephesians
6:13-17): breastplate, girdle, helmet, sword of the Spirit - and the great chain in the left hand.
"""
import math

from geo import Model
from materials import *
from parts import limb
from figure import item_bone, figure, back_wings, held_staff, base_animations, sin


def build():
    m = Model("geometry.fiatlux.power", visible_bounds=(4, 4, (0, 1.5, 0)))
    pts = figure(m, LINEN, ROBE_SHADE, GOLD, BERYL, BERYL_SHADE, LIGHTNING, TORCH_EYE, HAIR,
                 limb_mat=STEEL, limb_glow=GOLD, mantle=CRIMSON_CLOAK, mantle_len=22.0, hold={"r": 70})

    # breastplate of righteousness (Eph 6:14): plate, gold rims, a cross on the breast, tassets
    m.cube("torso", (-4.2, 21.6, -2.6), (8.4, 7.2, 5.2), STEEL, inflate=0.15)
    m.cube("torso", (-4.3, 28.4, -2.7), (8.6, 0.6, 5.4), GOLD, inflate=0.1)
    m.cube("torso", (-3.4, 21.4, -2.9), (6.8, 0.5, 0.4), GOLD)
    m.cube("torso", (-0.35, 22.6, -3.0), (0.7, 5.0, 0.3), GOLD_GLOW)
    m.cube("torso", (-1.6, 25.6, -3.0), (3.2, 0.7, 0.3), GOLD_GLOW)
    for k in range(4):
        m.cube("torso", (-3.9 + k * 2.0, 23.2, -2.85), (1.6, 0.3, 0.2), STEEL_DARK)
    for k in range(5):   # tassets over the robe
        x = -4.0 + k * 1.65
        tb = m.bone(f"tasset_{k}", (x + 0.8, 17.2, -2.3), "body")
        m.cube(tb, (x, 13.0, -2.6), (1.6, 4.2, 0.4), STEEL, rotation=(-6, 0, (k - 2) * 4), pivot=(x + 0.8, 17.2, -2.3))
        m.cube(tb, (x, 13.0, -2.75), (1.6, 0.4, 0.4), GOLD, rotation=(-6, 0, (k - 2) * 4), pivot=(x + 0.8, 17.2, -2.3))

    # pauldrons of three lames and vambraces
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        for k in range(3):
            m.cube(f"arm_{s}", (lo(3.6, 7.6 - k * 0.2), 27.0 - k * 1.3, -1.9 + k * 0.1), (4.0 - k * 0.2, 1.6, 3.8 - k * 0.2), STEEL,
                   rotation=(0, 0, side * (-12 - k * 6)), pivot=(X(5.0), 28.0, 0))
            m.cube(f"arm_{s}", (lo(3.6, 7.6 - k * 0.2), 27.0 - k * 1.3, -1.95 + k * 0.1), (4.0 - k * 0.2, 0.35, 3.9 - k * 0.2), GOLD,
                   rotation=(0, 0, side * (-12 - k * 6)), pivot=(X(5.0), 28.0, 0))
        m.cube(f"forearm_{s}", (lo(3.9, 7.1), 18.0, -1.6), (3.2, 4.0, 3.2), STEEL, inflate=0.05)
        m.cube(f"forearm_{s}", (lo(3.9, 7.1), 21.6, -1.6), (3.2, 0.5, 3.2), GOLD, inflate=0.1)
        m.cube(f"leg_{s}", (X(1.9) - 1.6, 2.2, -1.9), (3.2, 6.5, 0.6), STEEL)   # greaves
        m.cube(f"leg_{s}", (X(1.9) - 1.6, 8.4, -2.0), (3.2, 0.5, 0.7), GOLD)

    # helmet of salvation (Eph 6:17): open bowl, brow band, cheek guards, crest and a white plume
    hb = "head"
    m.cube(hb, (-3.8, 34.4, -3.9), (7.6, 3.2, 7.8), STEEL, inflate=0.15)
    m.cube(hb, (-3.9, 34.2, -4.05), (7.8, 0.6, 0.4), GOLD, inflate=0.05)
    for side in (1, -1):
        m.cube(hb, (side * 3.55 - (0.5 if side > 0 else 0), 30.8, -3.6), (0.5, 3.8, 3.4), STEEL, rotation=(0, side * -8, 0), pivot=(side * 3.6, 34.0, -2))
    m.cube(hb, (-0.4, 37.6, -3.6), (0.8, 1.2, 7.4), GOLD)
    plume = m.bone("plume", (0, 38.6, 0), "head")
    for k in range(7):
        z = -3.2 + k * 1.1
        m.cube(plume, (-0.45, 38.4, z), (0.9, 2.6 - abs(k - 3) * 0.25, 1.2), PLUME, rotation=(10 + k * 9, 0, 0), pivot=(0, 38.4, z))

    # sword of the Spirit (Eph 6:17): hilt, quillons, a blade edged with holy fire
    gx, gy, gz = pts["grip_r"]
    sw = item_bone(m, "sword", pts, "r")
    m.cube(sw, (gx - 0.4, gy - 2.6, gz - 0.4), (0.8, 4.4, 0.8), LEATHER)
    m.centered(sw, (gx, gy - 3.0, gz), (1.1, 1.1, 1.1), GOLD, rotation=(45, 0, 45), pivot=(gx, gy - 3.0, gz))
    m.cube(sw, (gx - 0.5, gy + 1.8, gz - 2.6), (1.0, 0.7, 5.2), GOLD)
    m.cube(sw, (gx - 0.35, gy + 2.5, gz - 0.9), (0.7, 15.0, 1.8), STEEL)
    m.cube(sw, (gx - 0.4, gy + 2.5, gz - 1.15), (0.8, 15.0, 0.3), HOLY_FIRE_WHITE)
    m.cube(sw, (gx - 0.4, gy + 2.5, gz + 0.85), (0.8, 15.0, 0.3), HOLY_FIRE_WHITE)
    m.centered(sw, (gx, gy + 17.8, gz), (0.6, 1.4, 1.2), STEEL, rotation=(45, 0, 0), pivot=(gx, gy + 17.8, gz))

    # the great chain (Rev 20:1): links swinging from the left fist, ending in an open fetter
    cx, cy, cz = pts["grip_l"]
    ch = m.bone("chain", pts["grip_l"], "hand_l")
    for k in range(9):
        y = cy - 0.8 - k * 1.05
        m.ring(ch, (cx, y, cz), 0.55, 6, (0.5, 0.3, 0.3), CHAIN, axis="z" if k % 2 else "x")
    fy = cy - 11.0
    m.ring(ch, (cx, fy, cz), 1.2, 10, (0.8, 0.5, 0.5), STEEL_DARK, axis="x")

    back_wings(m, POWER_FEATHER, POWER_FEATHER_SHADE, GOLD, scale=1.05)
    return m


def animations():
    extra = {"chain": {"rotation": [sin(80, 0, 6), 0, sin(60, 40, 5)]},
             "plume": {"rotation": [sin(70, 0, 4), 0, 0]}}
    move = dict(extra)
    move["chain"] = {"rotation": [f"25 + {sin(240, 0, 10)}", 0, 0]}
    judge = {   # the sword falls, and the chain is cast about the corrupt
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.2": [-150, 0, -10], "0.4": [30, 0, 0], "0.7": [0, 0, 0]}},
        "arm_l": {"rotation": {"0.0": [0, 0, 0], "0.3": [-90, -40, 20], "0.7": [0, 0, 0]}},
        "chain": {"rotation": {"0.0": [0, 0, 0], "0.3": [-80, 0, 40], "0.7": [0, 0, 0]}},
        "torso": {"rotation": {"0.0": [0, 0, 0], "0.2": [-8, -15, 0], "0.4": [12, 20, 0], "0.7": [0, 0, 0]}},
    }
    return base_animations("power", extra_idle=extra, extra_move=move, judge=judge, judge_len=0.7)
