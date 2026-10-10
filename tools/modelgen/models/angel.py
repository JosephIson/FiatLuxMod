"""The Angel - a messenger of the Lord as Scripture describes one (Tier 2).

Daniel 10:5-6: "a certain man clothed in linen, whose loins were girded with fine gold of
Uphaz: his body also was like the beryl, and his face as the appearance of lightning, and
his eyes as lamps of fire, and his arms and his feet like in colour to polished brass."
Matthew 28:3: "His countenance was like lightning, and his raiment white as snow."

Deliberately no wings and no halo: Scripture gives wings to seraphim and cherubim, while
messengers appear as men (Gen 18:2, Heb 13:2); the halo is a later artistic convention.
The divine radiance (Luke 2:9) is rendered as emissive layers plus the client FX aura.
"""
import math

from geo import Model
from materials import *
from parts import hand, head


def _arm(m, side):
    s = "l" if side > 0 else "r"
    X = lambda x: side * x
    lo = lambda a, b: min(X(a), X(b))
    arm = m.bone(f"arm_{s}", (X(5.0), 28.0, 0), "torso", rotation=(0, 0, side * -5))
    m.cube(arm, (lo(4.0, 7.0), 22.5, -1.5), (3, 6, 3), LINEN, inflate=0.2)
    m.cube(arm, (lo(4.0, 7.0), 26.2, -1.5), (3, 2.2, 3), LINEN, inflate=0.45)               # shoulder drape
    sleeve = m.bone(f"sleeve_{s}", (X(5.5), 23.0, 0), arm)                                     # hanging sleeve
    m.cube(sleeve, (lo(3.7, 7.3), 19.6, -1.9), (3.6, 3.6, 3.8), LINEN, rotation=(8, 0, 0), pivot=(X(5.5), 23.0, 0))
    m.cube(sleeve, (lo(3.7, 7.3), 19.3, -1.95), (3.6, 0.5, 3.9), LINEN, inflate=0.05, rotation=(8, 0, 0), pivot=(X(5.5), 23.0, 0))
    fore = m.bone(f"forearm_{s}", (X(5.5), 22.8, 0), arm)
    m.cube(fore, (lo(4.1, 6.9), 17.6, -1.4), (2.8, 5.2, 2.8), BRONZE)                        # arms like polished bronze
    m.cube(fore, (lo(4.0, 7.0), 18.4, -1.5), (3.0, 0.5, 3.0), BRONZE_GLOW, inflate=0.05)
    m.cube(fore, (lo(4.0, 7.0), 20.2, -1.5), (3.0, 0.5, 3.0), BRONZE_GLOW, inflate=0.05)
    hand(m, f"hand_{s}", side, fore, (X(5.5), 17.6, 0), BRONZE, BRONZE_GLOW, nail=BRONZE_GLOW)


def build():
    m = Model("geometry.fiatlux.angel", visible_bounds=(3, 3.5, (0, 1.4, 0)))
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 18, 0), "root")

    # loins girded with fine gold of Uphaz: a broad belt, buckle, and two hanging girdle ends
    m.cube("body", (-3.5, 17.0, -2.0), (7, 5, 4), LINEN)
    m.cube("body", (-3.7, 17.2, -2.2), (7.4, 1.8, 4.4), UPHAZ_GOLD, inflate=0.05)
    m.cube("body", (-1.3, 16.8, -2.8), (2.6, 2.4, 0.6), UPHAZ_GOLD)
    m.centered("body", (0, 18.0, -3.1), (1.0, 1.0, 0.4), BERYL_GLOW, rotation=(0, 0, 45), pivot=(0, 18.0, -3.1))
    for k in range(10):  # embossed studs around the girdle
        a = 2 * math.pi * (k + 0.5) / 10
        p = (math.sin(a) * 3.9, 18.1, -math.cos(a) * 2.45)
        m.centered("body", p, (0.5, 0.5, 0.5), UPHAZ_GOLD, rotation=(0, -math.degrees(a), 45), pivot=p)
    for side, s in ((1, "l"), (-1, "r")):
        g = m.bone(f"girdle_end_{s}", (side * 0.9, 17.0, -2.6), "body")
        m.cube(g, (side * 0.9 - 0.5, 9.5, -2.8), (1.0, 7.5, 0.3), UPHAZ_GOLD, rotation=(-3, 0, side * 4), pivot=(side * 0.9, 17.0, -2.6))
        m.cube(g, (side * 0.9 - 0.6, 9.0, -2.85), (1.2, 0.6, 0.4), UPHAZ_GOLD, rotation=(-3, 0, side * 4), pivot=(side * 0.9, 17.0, -2.6))

    # feet like polished bronze beneath the linen
    for side, s in ((1, "l"), (-1, "r")):
        lb = m.bone(f"leg_{s}", (side * 1.9, 17.0, 0), "body")
        x0 = side * 1.9 - 1.5
        m.cube(lb, (x0, 2.0, -1.5), (3, 15, 3), LINEN)
        foot = m.bone(f"foot_{s}", (side * 1.9, 2.0, 0), lb)
        m.cube(foot, (x0, 0.0, -2.6), (3, 2, 4.2), BRONZE)
        m.cube(foot, (x0 + 0.2, 0.0, -3.0), (2.6, 1.2, 0.6), BRONZE_GLOW)

    # linen robe "white as snow": eight boned skirt panels
    m.bone("skirt", (0, 17.0, 0), "body")
    for k in range(8):
        a = 2 * math.pi * k / 8
        px, pz = math.sin(a) * 3.4, -math.cos(a) * 2.6
        yaw = -math.degrees(a)
        pb = m.bone(f"robe_panel_{k}", (px, 17.0, pz), "skirt")
        flare = 8 if k in (0, 4) else 10
        rot = (flare * math.cos(a) * -1, yaw, flare * math.sin(a))
        m.cube(pb, (px - 1.7, 1.4, pz - 0.35), (3.4, 15.6, 0.7), LINEN, rotation=rot, pivot=(px, 17.0, pz))
        m.cube(pb, (px - 1.6, 1.4, pz - 0.5), (3.2, 15.0, 0.3), LINEN, rotation=rot, pivot=(px, 17.0, pz), inflate=0.1)
        m.cube(pb, (px - 1.75, 1.2, pz - 0.45), (3.5, 0.8, 0.9), LINEN, rotation=rot, pivot=(px, 17.0, pz), inflate=0.08)  # hem fold

    # torso and the outer linen mantle falling from the shoulders in six boned folds
    m.bone("torso", (0, 22.0, 0), "body")
    m.cube("torso", (-4.0, 21.8, -2.2), (8, 7, 4.4), LINEN)
    m.cube("torso", (-4.0, 21.8, -2.2), (8, 7, 4.4), LINEN, inflate=0.3)
    m.cube("torso", (-2.6, 24.0, -2.85), (5.2, 4.6, 0.4), LINEN, rotation=(0, 0, 0))
    m.cube("torso", (-0.4, 22.2, -2.95), (0.8, 6.4, 0.3), UPHAZ_GOLD)
    m.cube("torso", (-3.2, 28.4, -2.6), (6.4, 1.0, 5.0), LINEN, inflate=0.2)
    for k in range(5):  # fine linen pleats
        m.cube("torso", (-2.4 + k * 1.2, 22.0, -2.75), (0.35, 5.6, 0.25), LINEN, rotation=(0, 0, (k - 2) * 3), pivot=(-2.2 + k * 1.2, 27.6, -2.6))
    for side in (1, -1):  # gold clasps binding the mantle, with short chains
        m.centered("torso", (side * 3.4, 28.2, -2.5), (1.2, 1.2, 0.5), UPHAZ_GOLD, rotation=(0, 0, 45), pivot=(side * 3.4, 28.2, -2.5))
        m.centered("torso", (side * 3.4, 28.2, -2.8), (0.5, 0.5, 0.3), BERYL_GLOW)
        m.cube("torso", (side * 3.6 - 0.2, 26.4, 2.3), (0.4, 2.0, 0.4), UPHAZ_GOLD)
    m.bone("mantle", (0, 28.8, 2.4), "torso")
    for k in range(6):
        x = -4.2 + k * 1.45
        fb = m.bone(f"mantle_fold_{k}", (x + 0.7, 28.8, 2.4), "mantle")
        L = 24.0 + (k % 2) * 1.2
        m.cube(fb, (x, 28.8 - L, 2.2), (1.6, L, 0.6), LINEN, rotation=(6 + (k % 3), 0, (k - 2.5) * 1.5), pivot=(x + 0.7, 28.8, 2.4))
        m.cube(fb, (x - 0.05, 28.8 - L, 2.1), (1.7, 0.6, 0.8), UPHAZ_GOLD, rotation=(6 + (k % 3), 0, (k - 2.5) * 1.5), pivot=(x + 0.7, 28.8, 2.4))

    _arm(m, 1)
    _arm(m, -1)

    # body like beryl: neck and skin crystalline; face as the appearance of lightning
    m.bone("neck", (0, 29.0, 0), "torso")
    m.cube("neck", (-1.3, 28.8, -1.3), (2.6, 1.8, 2.6), BERYL)
    head(m, "head", "neck", (0, 30.4, 0), BERYL, LIGHTNING, BERYL_SHADE, LIGHTNING, TORCH_EYE)
    for side, s in ((1, "l"), (-1, "r")):  # eyes as lamps of fire: flames rising from each eye
        ex = side * 1.55
        flame = m.bone(f"eye_flame_{s}", (ex, 34.4, -3.7), "head")
        m.cube(flame, (ex - 0.45, 34.25, -3.85), (0.9, 1.3, 0.2), TORCH_EYE, rotation=(0, 0, side * -12), pivot=(ex, 34.3, -3.75))
        m.cube(flame, (ex - 0.25, 35.2, -3.85), (0.5, 0.8, 0.15), HOLY_FIRE_WHITE, rotation=(0, 0, side * -18), pivot=(ex, 34.3, -3.75))

    # hair: crown, side locks, and swaying back locks
    m.bone("hair", (0, 37.0, 0), "head")
    m.cube("hair", (-3.8, 36.3, -3.8), (7.6, 1.5, 7.6), HAIR)
    m.cube("hair", (-3.6, 35.7, -3.75), (7.2, 0.7, 1.0), HAIR)
    m.cube("hair", (-3.8, 33.5, 1.2), (7.6, 3.6, 2.7), HAIR)
    for side in (1, -1):
        m.cube("hair", (side * 3.7 - (0 if side > 0 else 0.9), 31.2, -3.4), (0.9, 5.5, 2.6), HAIR,
               rotation=(0, 0, side * -4), pivot=(side * 3.8, 36.5, -2))
    m.bone("hair_back", (0, 34.0, 3.6), "hair")
    for k in range(7):
        x = -3.3 + k * 1.1
        L = 6.5 + (k % 3) * 1.2
        m.cube("hair_back", (x, 34.0 - L, 3.4), (1.2, L, 0.9), HAIR, rotation=(10 + (k % 2) * 4, 0, (k - 3) * 2.5), pivot=(x + 0.6, 34.0, 3.8))
    return m


def animations():
    sin = lambda speed, phase=0, amp=1: f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    idle = {
        "body": {"position": [0, sin(90, 0, 0.5), 0]},
        "torso": {"rotation": [sin(90, 40, 1.2), 0, 0]},
        "head": {"rotation": [sin(45, 20, 3), sin(30, 0, 6), 0]},
        "hair_back": {"rotation": [sin(80, 60, 4), 0, sin(60, 0, 2)]},
        "arm_l": {"rotation": [sin(90, 30, 3), 0, sin(90, 0, 2)]},
        "arm_r": {"rotation": [sin(90, 210, 3), 0, sin(90, 180, 2)]},
        "sleeve_l": {"rotation": [sin(70, 0, 4), 0, 0]},
        "sleeve_r": {"rotation": [sin(70, 90, 4), 0, 0]},
        "eye_flame_l": {"scale": [1, f"1 + {sin(720, 0, 0.3)}", 1]},
        "eye_flame_r": {"scale": [1, f"1 + {sin(720, 90, 0.3)}", 1]},
        "head_eyelid_l": {"scale": {"0.0": [1, 1, 1], "3.7": [1, 1, 1], "3.8": [1, 3.4, 1], "3.9": [1, 1, 1]}},
        "head_eyelid_r": {"scale": {"0.0": [1, 1, 1], "3.7": [1, 1, 1], "3.8": [1, 3.4, 1], "3.9": [1, 1, 1]}},
        "girdle_end_l": {"rotation": [sin(60, 0, 3), 0, 0]},
        "girdle_end_r": {"rotation": [sin(60, 50, 3), 0, 0]},
    }
    for k in range(8):
        idle[f"robe_panel_{k}"] = {"rotation": [sin(70, k * 45, 2.5), 0, sin(55, k * 45 + 90, 1.5)]}
    for k in range(6):
        idle[f"mantle_fold_{k}"] = {"rotation": [sin(60, k * 40, 3), 0, sin(50, k * 60, 1.5)]}

    # Messengers ascend and descend (Gen 28:12) without wings: a weightless glide.
    glide = {
        "body": {"rotation": [8, 0, 0], "position": [0, sin(180, 0, 0.8), 0]},
        "leg_l": {"rotation": [10, 0, 0]},
        "leg_r": {"rotation": [14, 0, 0]},
        "arm_l": {"rotation": [-12, 0, 18]},
        "arm_r": {"rotation": [-12, 0, -18]},
        "hair_back": {"rotation": [24, 0, sin(240, 0, 3)]},
        "eye_flame_l": {"scale": [1, f"1.4 + {sin(900, 0, 0.3)}", 1]},
        "eye_flame_r": {"scale": [1, f"1.4 + {sin(900, 90, 0.3)}", 1]},
    }
    for k in range(8):
        glide[f"robe_panel_{k}"] = {"rotation": [f"{20 if k in (3, 4, 5) else 8} + {sin(240, k * 45, 4)}", 0, sin(240, k * 45, 2)]}
    for k in range(6):
        glide[f"mantle_fold_{k}"] = {"rotation": [f"38 + {sin(240, k * 40, 6)}", 0, sin(200, k * 60, 3)]}

    smite = {
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.25": [-160, 10, -10], "0.45": [30, 0, 0], "0.7": [0, 0, 0]}},
        "forearm_r": {"rotation": {"0.0": [0, 0, 0], "0.25": [-20, 0, 0], "0.45": [0, 0, 0]}},
        "torso": {"rotation": {"0.0": [0, 0, 0], "0.25": [-6, -14, 0], "0.45": [10, 18, 0], "0.7": [0, 0, 0]}},
        "mantle": {"rotation": {"0.0": [0, 0, 0], "0.3": [30, 0, 0], "0.7": [0, 0, 0]}},
    }
    return {
        "animation.angel.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.angel.glide": {"loop": True, "animation_length": 2.0, "bones": glide},
        "animation.angel.smite": {"loop": False, "animation_length": 0.7, "bones": smite},
    }
