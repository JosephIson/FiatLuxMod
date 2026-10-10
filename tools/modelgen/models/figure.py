"""Shared humanoid figure for the choirs of the second and third spheres.

Scripture names Dominions, Virtues, Powers and Principalities (Col 1:16, Eph 1:21, Eph 3:10,
Rom 8:38, 1 Pet 3:22) and the archangel (1 Thess 4:16, Jude 1:9) but does not describe how they
appear, beyond the "man clothed in linen" of Daniel 10:5-6. Each choir is therefore built on that
figure - linen, a girdle of gold, burnished limbs, a face of light - and distinguished by the
regalia the patristic tradition (Pseudo-Dionysius, Gregory the Great) assigns its office:
sceptre and orb, light and lilies, armour and chains, banner and crown, trumpet and spear.

figure() builds root -> body -> legs/feet, an eight-panel boned robe, a torso with a mantle of
folds, arms with sleeves, forearms and fully articulated hands, neck, head and hair, and returns a
dict of named attachment points (hand grips, back, crown) for the choir's own pieces.
"""
import math

from geo import Model
from materials import *
from parts import hand, head, limb, orient, wing


def figure(m, robe, robe_shade, trim, skin, skin_shade, face, eye_mat, hair, limb_mat=None, limb_glow=None,
           mantle=None, height=1.0, arm_spread=5.0, mantle_len=24.0, hold=None):
    """`hold` maps "l"/"r" to degrees a forearm is raised forward to carry something upright;
    item bones under that hand use item_bone(), which cancels the bend so the item can be
    modelled upright around the resting grip point."""
    hold = hold or {}
    limb_mat = limb_mat or skin
    limb_glow = limb_glow or trim
    mantle = mantle or robe
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 18, 0), "root")

    # girdle: belt, buckle, embossed studs and hanging ends
    m.cube("body", (-3.5, 17.0, -2.0), (7, 5, 4), robe)
    m.cube("body", (-3.7, 17.2, -2.2), (7.4, 1.8, 4.4), trim, inflate=0.05)
    m.cube("body", (-1.3, 16.8, -2.8), (2.6, 2.4, 0.6), trim)
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        p = (math.sin(a) * 3.9, 18.1, -math.cos(a) * 2.45)
        m.centered("body", p, (0.5, 0.5, 0.5), trim, rotation=(0, -math.degrees(a), 45), pivot=p)
    for side, s in ((1, "l"), (-1, "r")):
        g = m.bone(f"girdle_end_{s}", (side * 0.9, 17.0, -2.6), "body")
        m.cube(g, (side * 0.9 - 0.5, 10.5, -2.8), (1.0, 6.5, 0.3), trim, rotation=(-3, 0, side * 4), pivot=(side * 0.9, 17.0, -2.6))

    # legs and burnished feet
    for side, s in ((1, "l"), (-1, "r")):
        lb = m.bone(f"leg_{s}", (side * 1.9, 17.0, 0), "body")
        x0 = side * 1.9 - 1.5
        m.cube(lb, (x0, 2.0, -1.5), (3, 15, 3), robe_shade)
        foot = m.bone(f"foot_{s}", (side * 1.9, 2.0, 0), lb)
        m.cube(foot, (x0, 0.0, -2.6), (3, 2, 4.2), limb_mat)
        m.cube(foot, (x0 + 0.2, 0.0, -3.0), (2.6, 1.2, 0.6), limb_glow)

    # robe: eight boned skirt panels with hems
    m.bone("skirt", (0, 17.0, 0), "body")
    for k in range(8):
        a = 2 * math.pi * k / 8
        px, pz = math.sin(a) * 3.4, -math.cos(a) * 2.6
        pb = m.bone(f"robe_panel_{k}", (px, 17.0, pz), "skirt")
        flare = 8 if k in (0, 4) else 10
        rot = (flare * math.cos(a) * -1, -math.degrees(a), flare * math.sin(a))
        m.cube(pb, (px - 1.7, 1.4, pz - 0.35), (3.4, 15.6, 0.7), robe if k % 2 == 0 else robe_shade, rotation=rot, pivot=(px, 17.0, pz))
        m.cube(pb, (px - 1.75, 1.2, pz - 0.45), (3.5, 0.8, 0.9), trim, rotation=rot, pivot=(px, 17.0, pz), inflate=0.08)

    # torso, pleats and the mantle falling from the shoulders
    m.bone("torso", (0, 22.0, 0), "body")
    m.cube("torso", (-4.0, 21.8, -2.2), (8, 7, 4.4), robe)
    m.cube("torso", (-4.0, 21.8, -2.2), (8, 7, 4.4), robe, inflate=0.3)
    m.cube("torso", (-3.2, 28.4, -2.6), (6.4, 1.0, 5.0), robe, inflate=0.2)
    for k in range(5):
        m.cube("torso", (-2.4 + k * 1.2, 22.0, -2.75), (0.35, 5.6, 0.25), robe_shade, rotation=(0, 0, (k - 2) * 3), pivot=(-2.2 + k * 1.2, 27.6, -2.6))
    m.bone("mantle", (0, 28.8, 2.4), "torso")
    for k in range(6):
        x = -4.2 + k * 1.45
        fb = m.bone(f"mantle_fold_{k}", (x + 0.7, 28.8, 2.4), "mantle")
        L = mantle_len + (k % 2) * 1.2
        m.cube(fb, (x, 28.8 - L, 2.2), (1.6, L, 0.6), mantle, rotation=(6 + (k % 3), 0, (k - 2.5) * 1.5), pivot=(x + 0.7, 28.8, 2.4))
        m.cube(fb, (x - 0.05, 28.8 - L, 2.1), (1.7, 0.6, 0.8), trim, rotation=(6 + (k % 3), 0, (k - 2.5) * 1.5), pivot=(x + 0.7, 28.8, 2.4))

    # arms
    grips = {}
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        arm = m.bone(f"arm_{s}", (X(arm_spread), 28.0, 0), "torso", rotation=(0, 0, side * -5))
        m.cube(arm, (lo(4.0, 7.0), 22.5, -1.5), (3, 6, 3), robe, inflate=0.2)
        m.cube(arm, (lo(4.0, 7.0), 26.2, -1.5), (3, 2.2, 3), robe, inflate=0.45)
        sleeve = m.bone(f"sleeve_{s}", (X(5.5), 23.0, 0), arm)
        m.cube(sleeve, (lo(3.7, 7.3), 19.6, -1.9), (3.6, 3.6, 3.8), robe, rotation=(8, 0, 0), pivot=(X(5.5), 23.0, 0))
        m.cube(sleeve, (lo(3.7, 7.3), 19.3, -1.95), (3.6, 0.5, 3.9), trim, inflate=0.05, rotation=(8, 0, 0), pivot=(X(5.5), 23.0, 0))
        fore = m.bone(f"forearm_{s}", (X(5.5), 22.8, 0), arm, rotation=(-hold.get(s, 0), 0, 0))
        m.cube(fore, (lo(4.1, 6.9), 17.6, -1.4), (2.8, 5.2, 2.8), limb_mat)
        m.cube(fore, (lo(4.0, 7.0), 18.4, -1.5), (3.0, 0.5, 3.0), limb_glow, inflate=0.05)
        hand(m, f"hand_{s}", side, fore, (X(5.5), 17.6, 0), limb_mat, limb_glow, nail=limb_glow, curl=34.0)
        grips[s] = (X(5.5), 15.9, -0.2)     # centre of the closed fist, for held regalia

    m.bone("neck", (0, 29.0, 0), "torso")
    m.cube("neck", (-1.3, 28.8, -1.3), (2.6, 1.8, 2.6), skin)
    head(m, "head", "neck", (0, 30.4, 0), skin, face, skin_shade, face, eye_mat)

    m.bone("hair", (0, 37.0, 0), "head")
    m.cube("hair", (-3.8, 36.3, -3.8), (7.6, 1.5, 7.6), hair)
    m.cube("hair", (-3.6, 35.7, -3.75), (7.2, 0.7, 1.0), hair)
    m.cube("hair", (-3.8, 33.5, 1.2), (7.6, 3.6, 2.7), hair)
    for side in (1, -1):
        m.cube("hair", (side * 3.7 - (0 if side > 0 else 0.9), 31.2, -3.4), (0.9, 5.5, 2.6), hair,
               rotation=(0, 0, side * -4), pivot=(side * 3.8, 36.5, -2))
    m.bone("hair_back", (0, 34.0, 3.6), "hair")
    for k in range(6):
        x = -3.3 + k * 1.3
        L = 6.0 + (k % 3) * 1.2
        m.cube("hair_back", (x, 34.0 - L, 3.4), (1.3, L, 0.9), hair, rotation=(10 + (k % 2) * 4, 0, (k - 2.5) * 2.5), pivot=(x + 0.6, 34.0, 3.8))
    return {"grip_l": grips["l"], "grip_r": grips["r"], "crown": (0, 37.8, 0), "back": (0, 26.0, 2.6), "hold": hold}


def item_bone(m, name, pts, side):
    """A bone for regalia held in the `side` hand, modelled upright around the resting grip."""
    return m.bone(name, pts[f"grip_{side}"], f"hand_{side}", rotation=(pts["hold"].get(side, 0), 0, 0))


def back_wings(m, feather, shade, accent, scale=1.0, primaries=8, raise_=0.75, spread=0.45, fan=(10, 7)):
    """A pair of great feathered wings folded at the back, tips sweeping down behind the robe."""
    for side, s in ((1, "l"), (-1, "r")):
        wing(m, f"wing_{s}", side, "torso", (2.4, 28.2, 2.9), orient((spread, raise_, 0.55), (0.15, -1, 0.35)),
             scale=scale, primaries=primaries, feather=feather, shade=shade, accent=accent, fan=fan)


def held_staff(m, bone, grip, top, bottom, mat, thick=0.7, band=None):
    """A straight staff through the closed fist from `bottom` to `top` (y values), returns top point."""
    gx, gy, gz = grip
    limb(m, bone, (gx, bottom, gz), (gx, top, gz), thick, mat)
    if band is not None:
        for y in (gy + 1.4, gy - 1.4, bottom + 0.4):
            m.centered(bone, (gx, y, gz), (thick + 0.35, 0.4, thick + 0.35), band)
    return (gx, top, gz)


def crown(m, parent, center, radius, points, mat, gem=None, height=1.6, tilt=0.0):
    """A crown: a band of segments with upright points; gems set between the points."""
    cx, cy, cz = center
    cb = m.bone("crown", center, parent, rotation=(tilt, 0, 0))
    m.ring(cb, center, radius, points * 2, (radius * 0.5, 0.9, 0.5), mat)
    for k in range(points):
        a = 2 * math.pi * k / points
        px, pz = cx + math.cos(a) * radius, cz + math.sin(a) * radius
        m.centered(cb, (px, cy + 0.6 + height / 2, pz), (0.55, height, 0.55), mat, rotation=(0, -math.degrees(a), 0), pivot=(px, cy, pz))
        m.centered(cb, (px, cy + 0.9 + height, pz), (0.45, 0.45, 0.45), mat, rotation=(45, -math.degrees(a), 45), pivot=(px, cy + 0.9 + height, pz))
        if gem is not None:
            b = a + math.pi / points
            gx, gz = cx + math.cos(b) * (radius + 0.25), cz + math.sin(b) * (radius + 0.25)
            m.centered(cb, (gx, cy + 0.1, gz), (0.45, 0.45, 0.3), gem, rotation=(0, 90 - math.degrees(b), 45), pivot=(gx, cy + 0.1, gz))
    return cb


def sin(speed, phase=0, amp=1):
    return f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"


def base_animations(name, primaries=8, extra_idle=None, extra_move=None, judge=None, judge_len=0.8):
    """idle (breathing, robe and hair sway, slow wing breath, blinks), move (glide with wing beats)
    and judge (from `judge`, the choir's act of office)."""
    idle = {
        "body": {"position": [0, sin(90, 0, 0.5), 0]},
        "torso": {"rotation": [sin(90, 40, 1.2), 0, 0]},
        "head": {"rotation": [sin(45, 20, 3), sin(30, 0, 6), 0]},
        "hair_back": {"rotation": [sin(80, 60, 4), 0, sin(60, 0, 2)]},
        "arm_l": {"rotation": [sin(90, 30, 2), 0, sin(90, 0, 1.5)]},
        "arm_r": {"rotation": [sin(90, 210, 2), 0, sin(90, 180, 1.5)]},
        "head_eyelid_l": {"scale": {"0.0": [1, 1, 1], "3.7": [1, 1, 1], "3.8": [1, 3.4, 1], "3.9": [1, 1, 1]}},
        "head_eyelid_r": {"scale": {"0.0": [1, 1, 1], "3.7": [1, 1, 1], "3.8": [1, 3.4, 1], "3.9": [1, 1, 1]}},
    }
    for k in range(8):
        idle[f"robe_panel_{k}"] = {"rotation": [sin(70, k * 45, 2.5), 0, sin(55, k * 45 + 90, 1.5)]}
    for k in range(6):
        idle[f"mantle_fold_{k}"] = {"rotation": [sin(60, k * 40, 3), 0, sin(50, k * 60, 1.5)]}
    for s, sg in (("l", 1), ("r", -1)):
        idle[f"wing_{s}_arm"] = {"rotation": [0, 0, sin(90, 0, 3 * sg)]}
        for k in range(primaries):
            idle[f"wing_{s}_primary_{k}"] = {"rotation": [0, 0, sin(90, k * 20, 1.5 * sg)]}
    idle.update(extra_idle or {})

    move = {
        "body": {"rotation": [10, 0, 0], "position": [0, sin(240, 0, 0.8), 0]},
        "leg_l": {"rotation": [10, 0, 0]},
        "leg_r": {"rotation": [14, 0, 0]},
        "hair_back": {"rotation": [24, 0, sin(240, 0, 3)]},
    }
    for k in range(8):
        move[f"robe_panel_{k}"] = {"rotation": [f"{20 if k in (3, 4, 5) else 8} + {sin(240, k * 45, 4)}", 0, sin(240, k * 45, 2)]}
    for k in range(6):
        move[f"mantle_fold_{k}"] = {"rotation": [f"34 + {sin(240, k * 40, 6)}", 0, sin(200, k * 60, 3)]}
    for s, sg in (("l", 1), ("r", -1)):
        move[f"wing_{s}_root"] = {"rotation": [0, f"{sg} * -14", f"{sg} * ({sin(300, 0, 22)} + 12)"]}
        move[f"wing_{s}_arm"] = {"rotation": [0, 0, sin(300, 40, 8 * sg)]}
        for k in range(primaries):
            move[f"wing_{s}_primary_{k}"] = {"rotation": [0, 0, sin(300, 60 + k * 15, 5 * sg)]}
    move.update(extra_move or {})
    return {
        f"animation.{name}.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        f"animation.{name}.move": {"loop": True, "animation_length": 1.2, "bones": move},
        f"animation.{name}.judge": {"loop": False, "animation_length": judge_len, "bones": judge or {}},
    }
