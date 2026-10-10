"""The Principality (Archai) - first choir of the third sphere (Tier 2).

Colossians 1:16 and Ephesians 3:10 ("unto the principalities and powers in heavenly places");
Daniel 10:13, 20-21 speaks of the princes set over the nations ("the prince of the kingdom of
Persia", "Michael your prince"). Tradition gives the Principalities the guardianship of peoples,
cities and nations. Drawn as a prince of the heavenly court: a circlet surmounted by a cross,
a crimson cloak of royalty over linen, and the standard of the people in its keeping - a
swallow-tailed banner bearing a cross - with a short cross-headed sceptre in the other hand.
"""
import math

from geo import Model
from materials import *
from parts import limb
from figure import item_bone, figure, back_wings, held_staff, base_animations, sin


def build():
    m = Model("geometry.fiatlux.principality", visible_bounds=(4, 4.5, (0, 1.6, 0)))
    pts = figure(m, LINEN, ROBE_SHADE, UPHAZ_GOLD, BERYL, BERYL_SHADE, LIGHTNING, TORCH_EYE, HAIR,
                 limb_mat=BRONZE, limb_glow=BRONZE_GLOW, mantle=CRIMSON_CLOAK, mantle_len=25.0, hold={"r": 75})

    # a circlet of gold with a cross at the brow
    cb = m.bone("circlet", pts["crown"], "head")
    m.ring(cb, (0, 36.4, 0), 3.85, 16, (1.6, 0.6, 0.4), GOLD)
    m.centered(cb, (0, 38.0, -3.9), (0.5, 2.2, 0.4), GOLD_GLOW)
    m.centered(cb, (0, 38.4, -3.9), (1.6, 0.5, 0.4), GOLD_GLOW)
    m.centered(cb, (0, 36.4, -4.1), (0.7, 0.7, 0.3), SAPPHIRE, rotation=(0, 0, 45), pivot=(0, 36.4, -4.1))

    # the crimson cloak clasped across the shoulders
    m.cube("torso", (-4.4, 27.6, -2.6), (8.8, 1.4, 5.4), CRIMSON_CLOAK, inflate=0.25)
    for side in (1, -1):
        m.centered("torso", (side * 3.2, 28.2, -2.95), (1.1, 1.1, 0.5), UPHAZ_GOLD, rotation=(0, 0, 45), pivot=(side * 3.2, 28.2, -2.95))

    # the standard of the people: pole, crossbar, finial cross, and a swallow-tailed banner
    gx, gy, gz = pts["grip_r"]
    st = item_bone(m, "standard", pts, "r")
    held_staff(m, st, pts["grip_r"], 47.0, 1.0, WOOD, thick=0.6, band=GOLD)
    m.cube(st, (gx - 4.2, 45.4, gz - 0.3), (8.4, 0.6, 0.6), GOLD)
    m.centered(st, (gx, 48.4, gz), (0.45, 2.4, 0.45), GOLD_GLOW)
    m.centered(st, (gx, 48.8, gz), (1.6, 0.45, 0.45), GOLD_GLOW)
    bn = m.bone("banner", (gx, 45.4, gz), st)
    m.cube(bn, (gx - 3.9, 36.0, gz - 0.15), (7.8, 9.4, 0.3), BANNER)
    m.cube(bn, (gx - 0.6, 37.0, gz - 0.25), (1.2, 7.6, 0.2), BANNER_CROSS)
    m.cube(bn, (gx - 3.0, 41.6, gz - 0.25), (6.0, 1.2, 0.2), BANNER_CROSS)
    for side, s in ((1, "a"), (-1, "b")):
        tb = m.bone(f"banner_tail_{s}", (gx + side * 2.0, 36.0, gz), bn)
        m.cube(tb, (gx + side * 2.0 - 1.9, 31.5, gz - 0.15), (3.8, 4.5, 0.3), BANNER, rotation=(0, 0, side * 6), pivot=(gx + side * 2.0, 36.0, gz))
        m.cube(tb, (gx + side * 2.0 - 1.9, 31.5, gz - 0.2), (3.8, 0.5, 0.2), UPHAZ_GOLD, rotation=(0, 0, side * 6), pivot=(gx + side * 2.0, 36.0, gz))

    # a short sceptre headed with a cross
    sx, sy, sz = pts["grip_l"]
    sc = m.bone("sceptre", pts["grip_l"], "hand_l")
    limb(m, sc, (sx, sy + 1.5, sz), (sx + 0.6, sy - 7.5, sz - 2.4), 0.5, GOLD)   # carried lowered at the side
    m.centered(sc, (sx + 0.65, sy - 8.4, sz - 2.6), (0.4, 1.8, 0.4), GOLD_GLOW, rotation=(15, 0, 0), pivot=(sx + 0.65, sy - 8.4, sz - 2.6))
    m.centered(sc, (sx + 0.65, sy - 8.0, sz - 2.55), (1.4, 0.4, 0.4), GOLD_GLOW, rotation=(15, 0, 0), pivot=(sx + 0.65, sy - 8.0, sz - 2.55))

    back_wings(m, FEATHER, FEATHER_SHADE, FEATHER_GOLD, scale=1.0, primaries=7)
    return m


def animations():
    extra = {"banner": {"rotation": [sin(60, 0, 5), 0, sin(45, 30, 3)]},
             "banner_tail_a": {"rotation": [sin(90, 0, 8), 0, 0]},
             "banner_tail_b": {"rotation": [sin(90, 70, 8), 0, 0]}}
    move = dict(extra)
    move["banner"] = {"rotation": [f"25 + {sin(240, 0, 6)}", 0, 0]}
    judge = {   # the standard is planted and lifted high over the people
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.3": [-40, 0, -10], "0.6": [-40, 0, -10], "0.9": [0, 0, 0]}},
        "banner": {"rotation": {"0.0": [0, 0, 0], "0.3": [30, 0, 10], "0.9": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.3": [-15, 0, 0], "0.9": [0, 0, 0]}},
    }
    return base_animations("principality", primaries=7, extra_idle=extra, extra_move=move, judge=judge, judge_len=0.9)
