"""The Dominion (Kyriotetes) - first choir of the second sphere (Tier 3).

Colossians 1:16: "For by him were all things created ... whether they be thrones, or dominions,
or principalities, or powers." Tradition (Pseudo-Dionysius, Celestial Hierarchy 8) gives the
Dominions the governance of the lower angels' offices: lordship exercised in service. They are
drawn as the man in linen of Daniel 10 vested for rule - a stole of sapphire (Exodus 24:10, the
pavement beneath the throne) embroidered with gold, a crown, the sceptre of governance in the
right hand and the orb of the world, surmounted by a cross, in the left; great wings folded behind.
"""
import math

from geo import Model
from materials import *
from parts import limb
from figure import item_bone, figure, back_wings, held_staff, crown, base_animations, sin


def build():
    m = Model("geometry.fiatlux.dominion", visible_bounds=(4, 4, (0, 1.5, 0)))
    pts = figure(m, LINEN, ROBE_SHADE, UPHAZ_GOLD, BERYL, BERYL_SHADE, LIGHTNING, TORCH_EYE, HAIR,
                 limb_mat=BRONZE, limb_glow=BRONZE_GLOW, mantle=DOMINION_STOLE, hold={"r": 80, "l": 70})

    # the sapphire stole: two bands from the shoulders to the hem, gold crosses and a fringe
    for side in (1, -1):
        x = side * 1.9
        m.cube("torso", (x - 0.8, 21.8, -2.75), (1.6, 7.0, 0.35), DOMINION_STOLE)
        m.cube("body", (x - 0.8, 4.0, -3.15), (1.6, 13.0, 0.35), DOMINION_STOLE, rotation=(-6, 0, 0), pivot=(x, 17, -2.8))
        for y in (25.0, 12.5, 7.0):
            bone = "torso" if y > 20 else "body"
            z = -2.98 if y > 20 else -3.45 + (17 - y) * 0.1
            m.cube(bone, (x - 0.15, y - 0.9, z), (0.3, 1.8, 0.2), UPHAZ_GOLD)
            m.cube(bone, (x - 0.6, y - 0.15, z), (1.2, 0.3, 0.2), UPHAZ_GOLD)
        for k in range(4):
            m.cube("body", (x - 0.75 + k * 0.42, 3.0, -3.3), (0.22, 1.1, 0.2), UPHAZ_GOLD, rotation=(-6, 0, 0), pivot=(x, 17, -2.8))
    # a jewelled clasp at the breast
    m.centered("torso", (0, 27.6, -2.9), (1.6, 1.6, 0.5), UPHAZ_GOLD, rotation=(0, 0, 45), pivot=(0, 27.6, -2.9))
    m.centered("torso", (0, 27.6, -3.2), (0.8, 0.8, 0.3), SAPPHIRE, rotation=(0, 0, 45), pivot=(0, 27.6, -3.2))

    crown(m, "head", pts["crown"], 3.9, 8, GOLD, gem=SAPPHIRE, height=1.8)
    m.centered("crown", (0, 41.2, -0.2), (1.2, 1.2, 1.2), SAPPHIRE, rotation=(45, 45, 0), pivot=(0, 41.2, -0.2))

    # sceptre of governance: gold rod, collars, and a finial of a sapphire held in a lily of gold
    gx, gy, gz = pts["grip_r"]
    sc = item_bone(m, "sceptre", pts, "r")
    held_staff(m, sc, pts["grip_r"], 31.0, 9.0, GOLD, thick=0.75, band=UPHAZ_GOLD)
    for k in range(4):   # the lily cup cradling the stone
        a = k * math.pi / 2 + math.pi / 4
        limb(m, sc, (gx, 31.0, gz), (gx + math.cos(a) * 1.4, 33.0, gz + math.sin(a) * 1.4), 0.5, GOLD)
    m.centered(sc, (gx, 33.0, gz), (1.6, 1.6, 1.6), SAPPHIRE, rotation=(45, 45, 0), pivot=(gx, 33.0, gz))
    m.centered(sc, (gx, 34.6, gz), (0.6, 1.2, 0.6), GOLD_GLOW)

    # the orb of the world surmounted by a cross (globus cruciger), carried in the left hand
    ox, oy, oz = pts["grip_l"]
    orb = item_bone(m, "orb", pts, "l")
    c = (ox, oy + 2.6, oz)
    for rot in ((0, 0, 0), (0, 45, 0), (45, 0, 0)):
        m.centered(orb, c, (2.6, 2.6, 2.6), GOLD, rotation=rot, pivot=c)
    m.centered(orb, c, (2.9, 0.5, 2.9), SAPPHIRE, rotation=(0, 45, 0), pivot=c)
    m.centered(orb, (c[0], c[1] + 2.2, c[2]), (0.45, 1.8, 0.45), GOLD_GLOW)
    m.centered(orb, (c[0], c[1] + 2.5, c[2]), (1.3, 0.45, 0.45), GOLD_GLOW)

    back_wings(m, FEATHER, FEATHER_SHADE, FEATHER_GOLD, scale=1.1)
    return m


def animations():
    judge = {   # the sceptre is raised, and the decree goes forth
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.3": [-150, 0, -12], "0.6": [-150, 0, -12], "0.9": [0, 0, 0]}},
        "torso": {"rotation": {"0.0": [0, 0, 0], "0.3": [-6, 0, 0], "0.9": [0, 0, 0]}},
        "wing_l_root": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, 25], "0.9": [0, 0, 0]}},
        "wing_r_root": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, -25], "0.9": [0, 0, 0]}},
    }
    extra = {}
    return base_animations("dominion", extra_idle=extra, extra_move=extra, judge=judge, judge_len=0.9)
