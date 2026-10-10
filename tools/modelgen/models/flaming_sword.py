"""The Flaming Sword of Cherubim (Genesis 3:24): golden carved crossguard of folded cherub
wings, ivory hilt bound in gold, a translucent crystal blade wreathed in emissive holy fire."""
import math

from geo import Model, write_animations
from materials import *


def build():
    m = Model("geometry.fiatlux.flaming_sword_of_cherubim", visible_bounds=(3, 4, (0, 1.2, 0)))
    m.bone("root", (0, 0, 0))

    # --- pommel: gold orb, sapphire heart, four prongs ---------------------------------
    m.bone("pommel", (0, 2, 0), "root")
    m.centered("pommel", (0, 1.6, 0), (2.4, 2.4, 2.4), GOLD, rotation=(45, 0, 45), pivot=(0, 1.6, 0))
    m.centered("pommel", (0, 1.6, 0), (1.4, 1.4, 1.4), SAPPHIRE, inflate=0.55)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        p = (math.cos(a) * 1.3, 2.6, math.sin(a) * 1.3)
        m.centered("pommel", p, (0.5, 1.6, 0.5), GOLD_DARK, rotation=(math.sin(a) * 25, 0, -math.cos(a) * 25), pivot=p)
    m.centered("pommel", (0, 0.2, 0), (0.6, 0.8, 0.6), GOLD, rotation=(0, 45, 0), pivot=(0, 0.2, 0))

    # --- grip: ivory core with gold bands and a diagonal leather-gold wrap ---------------
    m.bone("grip", (0, 3.5, 0), "pommel")
    m.cube("grip", (-0.9, 3.4, -0.9), (1.8, 8.2, 1.8), IVORY)
    for i, y in enumerate((3.6, 6.0, 8.4, 10.8)):
        m.cube("grip", (-1.05, y, -1.05), (2.1, 0.6, 2.1), GOLD if i % 3 == 0 else GOLD_DARK, inflate=0.05)
    for i in range(6):
        y = 4.3 + i * 1.15
        m.centered("grip", (0, y, 0), (2.0, 0.35, 2.0), IVORY_WRAP, rotation=(0, 0, 18 if i % 2 else -18), pivot=(0, y, 0), inflate=-0.02)

    # --- guard: central reliquary + two folded cherub wings sweeping toward the blade ----
    m.bone("guard", (0, 12, 0), "grip")
    m.cube("guard", (-2.2, 11.4, -1.4), (4.4, 2.0, 2.8), GOLD)
    m.cube("guard", (-1.6, 13.2, -1.1), (3.2, 0.8, 2.2), GOLD_DARK)
    m.centered("guard", (0, 12.4, -1.55), (1.2, 1.2, 0.5), SAPPHIRE, rotation=(0, 0, 45), pivot=(0, 12.4, -1.55))
    m.centered("guard", (0, 12.4, 1.55), (1.2, 1.2, 0.5), SAPPHIRE, rotation=(0, 0, 45), pivot=(0, 12.4, 1.55))
    m.ring("guard", (0, 12.4, -1.75), 1.25, 8, (0.45, 0.3, 0.3), GOLD_GLOW, axis="z")
    m.ring("guard", (0, 12.4, 1.75), 1.25, 8, (0.45, 0.3, 0.3), GOLD_GLOW, axis="z")
    for side in (-1, 1):
        wing = m.bone(f"guard_wing_{'l' if side > 0 else 'r'}", (side * 2.0, 12.4, 0), "guard")
        # five stacked feather blades rising like the wings over the mercy seat
        for k in range(5):
            length = 5.4 - k * 0.7
            ang = 64 - k * 9
            px = side * (2.0 + k * 0.15)
            py = 11.6 + k * 0.55
            m.cube(wing, (px if side > 0 else px - length, py, -0.9 + k * 0.04), (length, 0.9, 1.8 - k * 0.15),
                   GOLD if k % 2 == 0 else GOLD_DARK, rotation=(0, 0, -side * ang), pivot=(px, py + 0.45, 0))
        tip = (side * (2.0 + 5.4 * math.cos(math.radians(64))), 11.6 + 0.45 + 5.4 * math.sin(math.radians(64)), 0)
        m.centered(wing, tip, (0.9, 0.9, 0.9), GOLD_GLOW, rotation=(45, 45, 0), pivot=tip)
        for k in range(3):
            y = 11.9 + k * 0.8
            m.cube(wing, (side * 2.2 - (0 if side > 0 else 1.6), y, -1.0), (1.6, 0.3, 2.0), RUNE_GOLD,
                   rotation=(0, 0, -side * 30), pivot=(side * 2.2, y, 0))

    # --- blade: crystal body, bevelled edges, glowing fuller, tapered point ------------
    m.bone("blade", (0, 14, 0), "guard")
    m.cube("blade", (-1.5, 14.0, -0.45), (3.0, 22.0, 0.9), CRYSTAL)
    m.cube("blade", (-0.35, 14.4, -0.55), (0.7, 20.5, 1.1), CRYSTAL_CORE)  # fuller of living light
    for side in (-1, 1):
        x = side * 1.5
        m.cube("blade", (x - 0.35, 14.0, -0.35), (0.7, 22.0, 0.7), CRYSTAL, rotation=(0, 45, 0), pivot=(x, 14, 0), inflate=-0.05)
    taper = [(36.0, 2.5, 1.6), (37.6, 1.8, 1.4), (39.0, 1.1, 1.2)]
    for y, w, h in taper:
        m.cube("blade", (-w / 2, y, -0.4), (w, h, 0.8), CRYSTAL)
    m.centered("blade", (0, 40.6, 0), (0.9, 0.9, 0.7), CRYSTAL_CORE, rotation=(0, 0, 45), pivot=(0, 40.6, 0))
    for k in range(6):  # engraved runes climbing the blade
        y = 16 + k * 3.2
        m.cube("blade", (-0.9, y, -0.62), (1.8, 0.25, 0.1), RUNE_GOLD)
        m.cube("blade", (-0.9, y, 0.52), (1.8, 0.25, 0.1), RUNE_GOLD)

    # --- holy fire: three flickering flame sheets that the animation sways and pulses ---
    for name, side in (("fire_l", 1), ("fire_r", -1)):
        m.bone(name, (side * 1.6, 14.5, 0), "blade")
        for k in range(9):
            y = 15.0 + k * 2.6
            h = 3.4 - (k % 3) * 0.6
            lean = side * (10 + (k * 37) % 17)
            mat = HOLY_FIRE if k % 2 == 0 else HOLY_FIRE_WHITE
            m.cube(name, (side * 1.5 - (0 if side > 0 else 1.3), y, -0.3), (1.3, h, 0.6), mat,
                   rotation=(0, 0, -lean), pivot=(side * 1.5, y, 0))
    m.bone("fire_tip", (0, 39, 0), "blade")
    for k in range(4):
        p = (0, 40.2 + k * 0.9, 0)
        m.centered("fire_tip", p, (1.4 - k * 0.3, 1.6, 0.6), HOLY_FIRE_WHITE if k % 2 else HOLY_FIRE,
                   rotation=(0, k * 30, (k - 1.5) * 12), pivot=p)
    return m


def animations():
    flicker = lambda speed, phase, amp: f"1 + math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    return {
        "animation.flaming_sword_of_cherubim.idle": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "fire_l": {
                    "scale": [flicker(540, 0, 0.12), flicker(720, 40, 0.18), 1],
                    "rotation": [0, 0, "math.sin(query.anim_time * 360) * 4"],
                },
                "fire_r": {
                    "scale": [flicker(540, 90, 0.12), flicker(720, 130, 0.18), 1],
                    "rotation": [0, 0, "math.sin(query.anim_time * 360 + 90) * -4"],
                },
                "fire_tip": {
                    "scale": [flicker(900, 0, 0.15), flicker(600, 60, 0.25), flicker(900, 0, 0.15)],
                    "rotation": [0, "query.anim_time * 180", 0],
                },
                "pommel": {"scale": [1, 1, 1]},
            },
        },
        "animation.flaming_sword_of_cherubim.ignite": {
            "loop": False,
            "animation_length": 0.6,
            "bones": {
                "fire_l": {"scale": {"0.0": [0.2, 0.2, 1], "0.3": [1.6, 1.4, 1], "0.6": [1, 1, 1]}},
                "fire_r": {"scale": {"0.0": [0.2, 0.2, 1], "0.3": [1.6, 1.4, 1], "0.6": [1, 1, 1]}},
                "fire_tip": {"scale": {"0.0": [0.2, 0.2, 0.2], "0.35": [2.2, 2.6, 2.2], "0.6": [1, 1, 1]}},
            },
        },
    }
