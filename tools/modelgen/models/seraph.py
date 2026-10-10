"""Seraph - "the burning ones" who stand above the throne (Tier 3).

Isaiah 6:2-3: "Above it stood the seraphims: each one had six wings; with twain he covered
his face, and with twain he covered his feet, and with twain he did fly. And one cried unto
another, and said, Holy, holy, holy, is the LORD of hosts."
Isaiah 6:6: "Then flew one of the seraphims unto me, having a live coal in his hand, which
he had taken with the tongs from off the altar."

Six wings of living flame (upper pair veils the face, lower pair veils the feet, middle pair
flies), a robed body of fire, and tongs bearing a live coal from the altar.
"""
import math

from geo import Model
from materials import *
from parts import hand, head, orient, veil_wing, wing, wing_bones

# The flying pair spreads from the back. The veiling pairs (see parts.veil_wing) arch forward
# from the back and drop overlapping feather curtains over the face and over the feet.
MIDDLE = dict(shoulder=(2.2, 26.5, 2.6), rotation=orient((1, 0.55, 0.6), (0, -1, 0.3)), scale=1.0)
FACE_VEIL = dict(root=(2.4, 31.5, 2.2), wrist=(3.6, 39.5, -5.0), bar_y=39.2, front_z=-5.2,
                 inner_x=-1.2, outer_x=4.6, length=11.0, feathers=7)
FEET_VEIL = dict(root=(3.0, 16.5, 1.4), wrist=(4.0, 13.8, -4.2), bar_y=13.6, front_z=-4.4,
                 inner_x=-1.2, outer_x=4.4, length=14.6, feathers=7)


def build():
    m = Model("geometry.fiatlux.seraph", visible_bounds=(6, 5, (0, 1.6, 0)))
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 17, 0), "root")

    # a column of fire robed in light: layered flame skirt with boned tongues
    m.cube("body", (-3.4, 15.5, -2.0), (6.8, 6.5, 4), SERAPH_CORE)
    m.cube("body", (-3.6, 17.0, -2.2), (7.2, 1.4, 4.4), UPHAZ_GOLD, inflate=0.05)
    m.bone("flame_skirt", (0, 16.0, 0), "body")
    for k in range(10):
        a = 2 * math.pi * k / 10
        px, pz = math.sin(a) * 3.0, -math.cos(a) * 2.2
        tb = m.bone(f"flame_tongue_{k}", (px, 16.0, pz), "flame_skirt")
        L = 13.0 + (k % 3) * 1.5
        rot = (-7 * math.cos(a), -math.degrees(a), 7 * math.sin(a))
        m.cube(tb, (px - 1.4, 16.0 - L, pz - 0.4), (2.8, L, 0.8), SERAPH_FEATHER if k % 2 else SERAPH_SHADE, rotation=rot, pivot=(px, 16.0, pz))
        m.cube(tb, (px - 0.8, 16.0 - L - 1.6, pz - 0.3), (1.6, 2.0, 0.6), HOLY_FIRE_WHITE, rotation=rot, pivot=(px, 16.0, pz))

    for side, s in ((1, "l"), (-1, "r")):
        lb = m.bone(f"leg_{s}", (side * 1.8, 16.0, 0), "body")
        m.cube(lb, (side * 1.8 - 1.4, 2.0, -1.4), (2.8, 14, 2.8), SERAPH_CORE)
        m.cube(lb, (side * 1.8 - 1.4, 0.0, -2.4), (2.8, 2.0, 3.8), BRONZE_GLOW)

    m.bone("torso", (0, 21.5, 0), "body")
    m.cube("torso", (-4.0, 21.5, -2.2), (8, 7.2, 4.4), SERAPH_CORE)
    m.cube("torso", (-4.0, 21.5, -2.2), (8, 7.2, 4.4), SERAPH_FEATHER, inflate=0.3)
    m.cube("torso", (-1.5, 23.5, -2.9), (3.0, 3.0, 0.6), EMBER)
    m.ring("torso", (0, 25.0, -3.1), 2.1, 10, (0.7, 0.35, 0.3), UPHAZ_GOLD, axis="z")

    # arms; the right hand bears the tongs with a live coal from the altar
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        arm = m.bone(f"arm_{s}", (X(5.0), 28.0, 0), "torso", rotation=(-30 if side < 0 else 0, 0, side * -6))
        m.cube(arm, (lo(4.0, 7.0), 22.5, -1.5), (3, 6, 3), SERAPH_FEATHER, inflate=0.2)
        fore = m.bone(f"forearm_{s}", (X(5.5), 22.8, 0), arm, rotation=(-40 if side < 0 else 0, 0, 0))
        m.cube(fore, (lo(4.1, 6.9), 17.6, -1.4), (2.8, 5.2, 2.8), SERAPH_CORE)
        m.cube(fore, (lo(4.0, 7.0), 18.2, -1.5), (3.0, 1.8, 3.0), UPHAZ_GOLD, inflate=0.1)
        hand(m, f"hand_{s}", side, fore, (X(5.5), 17.6, 0), SERAPH_SKIN, SERAPH_SKIN_SHADE, nail=UPHAZ_GOLD)
    tongs = m.bone("tongs", (-5.5, 15.0, -0.6), "hand_r")
    m.cube(tongs, (-6.0, 6.5, -0.9), (0.5, 9.0, 0.5), BRONZE, rotation=(0, 0, 6), pivot=(-5.75, 15.0, -0.6))
    m.cube(tongs, (-5.4, 6.5, -0.9), (0.5, 9.0, 0.5), BRONZE, rotation=(0, 0, -6), pivot=(-5.15, 15.0, -0.6))
    m.cube(tongs, (-6.3, 14.2, -1.0), (1.6, 0.8, 0.8), BRONZE_GLOW)
    coal = m.bone("live_coal", (-5.6, 6.2, -0.7), "tongs")
    m.centered(coal, (-5.6, 6.2, -0.7), (1.6, 1.4, 1.5), EMBER, rotation=(20, 35, 10), pivot=(-5.6, 6.2, -0.7))
    m.centered(coal, (-5.6, 6.2, -0.7), (1.0, 1.0, 1.0), HOLY_FIRE_WHITE, inflate=0.45)

    # head of white fire (veiled by the upper wings)
    m.bone("neck", (0, 28.7, 0), "torso")
    m.cube("neck", (-1.3, 28.5, -1.3), (2.6, 1.8, 2.6), SERAPH_CORE)
    head(m, "head", "neck", (0, 30.1, 0), SERAPH_SKIN, LIGHTNING, SERAPH_SKIN_SHADE, EMBER, TORCH_EYE)
    m.bone("crown_flames", (0, 37.0, 0), "head")
    for k in range(7):
        a = 2 * math.pi * k / 7
        p = (math.sin(a) * 2.4, 37.6, -math.cos(a) * 2.4)
        m.centered("crown_flames", p, (1.2, 2.6 + (k % 2), 1.2), HOLY_FIRE_WHITE if k % 2 else HOLY_FIRE,
                   rotation=(-12 * math.cos(a), 0, 12 * math.sin(a)), pivot=(p[0], 36.5, p[2]))

    # six wings
    for side, s in ((1, "l"), (-1, "r")):
        veil_wing(m, f"wing_upper_{s}", side, "torso", **FACE_VEIL,
                  feather=SERAPH_FEATHER, shade=SERAPH_SHADE, accent=SERAPH_CORE)
        wing(m, f"wing_mid_{s}", side, "torso", **MIDDLE, primaries=8,
             feather=SERAPH_FEATHER, shade=SERAPH_SHADE, accent=SERAPH_CORE)
        veil_wing(m, f"wing_lower_{s}", side, "body", **FEET_VEIL,
                  feather=SERAPH_FEATHER, shade=SERAPH_SHADE, accent=SERAPH_CORE)
    return m


def animations():
    sin = lambda speed, phase=0, amp=1: f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    hover = {
        "root": {"position": [0, f"4 + {sin(120, 0, 1.2)}", 0]},
        "torso": {"rotation": [sin(120, 40, 1.5), 0, 0]},
        "crown_flames": {"scale": [1, f"1 + {sin(600, 0, 0.25)}", 1]},
        "live_coal": {"scale": [f"1 + {sin(480, 0, 0.12)}"] * 3},
    }
    for k in range(10):
        hover[f"flame_tongue_{k}"] = {"rotation": [sin(300, k * 36, 5), 0, sin(240, k * 36 + 90, 3)],
                                      "scale": [1, f"1 + {sin(420, k * 50, 0.12)}", 1]}
    for s, sign in (("l", 1), ("r", -1)):
        # the flying pair beats; the veiling pairs tremble but keep covering face and feet
        hover[f"wing_mid_{s}_arm"] = {"rotation": [0, sin(240, 0, 8 * sign), sin(240, 0, 34 * sign)]}
        hover[f"wing_mid_{s}_fore"] = {"rotation": [0, 0, sin(240, -40, 16 * sign)]}
        hover[f"wing_mid_{s}_hand"] = {"rotation": [0, 0, sin(240, -80, 20 * sign)]}
        for k in range(8):
            hover[f"wing_mid_{s}_primary_{k}"] = {"rotation": [0, 0, sin(240, -100 - k * 10, (3 + k) * sign)]}
        for pair in ("upper", "lower"):
            # veiling feathers only tremble - they never uncover the face or feet
            for k in range(7):
                hover[f"wing_{pair}_{s}_feather_{k}"] = {"rotation": [sin(480, k * 30, 1.5), 0, 0]}
    # "Holy, holy, holy": three swells of fire and wings
    cry = {
        "torso": {"rotation": {"0.0": [0, 0, 0], "0.3": [-12, 0, 0], "0.6": [0, 0, 0], "0.9": [-12, 0, 0], "1.2": [0, 0, 0], "1.5": [-12, 0, 0], "1.8": [0, 0, 0]}},
        "crown_flames": {"scale": {"0.0": [1, 1, 1], "0.3": [1.3, 2.2, 1.3], "0.6": [1, 1, 1], "0.9": [1.3, 2.2, 1.3], "1.2": [1, 1, 1], "1.5": [1.3, 2.2, 1.3], "1.8": [1, 1, 1]}},
    }
    for s, sign in (("l", 1), ("r", -1)):
        cry[f"wing_mid_{s}_arm"] = {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, 40 * sign], "0.6": [0, 0, 0], "0.9": [0, 0, 40 * sign], "1.2": [0, 0, 0], "1.5": [0, 0, 40 * sign], "1.8": [0, 0, 0]}}
    return {
        "animation.seraph.hover": {"loop": True, "animation_length": 3.0, "bones": hover},
        "animation.seraph.holy": {"loop": False, "animation_length": 1.8, "bones": cry},
    }
