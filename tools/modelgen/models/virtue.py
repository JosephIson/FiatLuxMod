"""The Virtue (Dynameis) - second choir of the second sphere (Tier 3).

Ephesians 1:21 and 1 Peter 3:22 name the "powers" and "mights" set under Christ; tradition
(Gregory the Great, Homily 34) calls the Virtues "those spirits through whom signs and miracles
are most frequently done". They are drawn as the man in linen made wholly of light - "the
glory of the Lord shone round about them" (Luke 2:9) - a robe of radiance, a sunburst of rays
behind the head, the lily of grace in one hand and the other raised in blessing.
"""
import math

from geo import Model
from materials import *
from parts import limb
from figure import item_bone, figure, back_wings, base_animations, sin


def build():
    m = Model("geometry.fiatlux.virtue", visible_bounds=(4, 4, (0, 1.5, 0)))
    pts = figure(m, LIGHT_ROBE, LIGHT_ROBE_SHADE, GOLD_GLOW, SERAPH_SKIN, SERAPH_SKIN_SHADE, LIGHTNING, EYE_GLOW, HAIR,
                 limb_mat=SERAPH_SKIN, limb_glow=GOLD_GLOW, mantle=LIGHT_ROBE, hold={"r": 75})

    # sunburst behind the head: sixteen rays, long and short, around a glowing disc
    sb = m.bone("sunburst", (0, 34.0, 4.6), "head")
    m.ring(sb, (0, 34.0, 4.6), 3.2, 16, (1.4, 0.5, 0.4), GOLD_GLOW, axis="z")
    m.centered(sb, (0, 34.0, 4.75), (4.4, 4.4, 0.2), HOLY_FIRE_WHITE, rotation=(0, 0, 45), pivot=(0, 34.0, 4.75))
    for k in range(16):
        a = 2 * math.pi * k / 16
        L = 7.5 if k % 2 == 0 else 4.5
        r0 = 3.6
        limb(m, sb, (math.cos(a) * r0, 34.0 + math.sin(a) * r0, 4.7),
             (math.cos(a) * (r0 + L), 34.0 + math.sin(a) * (r0 + L), 4.7), 0.6 if k % 2 == 0 else 0.45, SUNRAY)

    # the lily of grace in the right hand (Song 2:2, Hosea 14:5): stem, leaves, six petals, stamens
    gx, gy, gz = pts["grip_r"]
    ly = item_bone(m, "lily", pts, "r")
    limb(m, ly, (gx, gy - 2.5, gz), (gx, 30.0, gz - 1.0), 0.4, LILY_STEM)
    for k, (y, d) in enumerate(((22.0, 1), (25.0, -1), (27.5, 1))):
        limb(m, ly, (gx, y, gz - 0.5), (gx + d * 1.6, y + 2.0, gz - 0.9), 0.35, LILY_STEM)
    top = (gx, 30.0, gz - 1.0)
    for k in range(6):
        a = 2 * math.pi * k / 6
        tip = (gx + math.cos(a) * 1.6, 31.9, gz - 1.0 + math.sin(a) * 1.6)
        limb(m, ly, top, tip, 0.6, LILY)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.5
        limb(m, ly, top, (gx + math.cos(a) * 0.6, 31.6, gz - 1.0 + math.sin(a) * 0.6), 0.2, GOLD_GLOW)

    # motes of light orbiting the raised left hand
    gl = m.bone("blessing", pts["grip_l"], "hand_l")
    bx, by, bz = pts["grip_l"]
    for k in range(5):
        a = 2 * math.pi * k / 5
        m.centered(gl, (bx + math.cos(a) * 1.6, by + 0.5, bz + math.sin(a) * 1.6), (0.5, 0.5, 0.5), HOLY_FIRE_WHITE,
                   rotation=(45, 45, 0), pivot=(bx + math.cos(a) * 1.6, by + 0.5, bz + math.sin(a) * 1.6))

    back_wings(m, VIRTUE_FEATHER, FEATHER_GOLD, GOLD_GLOW, scale=1.1)
    return m


def animations():
    extra = {
        "arm_l": {"rotation": [f"-55 + {sin(90, 30, 3)}", 0, 10]},   # the hand raised in blessing
        "forearm_l": {"rotation": [-50, 0, 0]},
        "sunburst": {"rotation": [0, 0, "query.anim_time * 20"]},
        "blessing": {"rotation": [0, "query.anim_time * 90", 0]},
    }
    judge = {   # a sign: both hands lifted and light poured out
        "arm_l": {"rotation": {"0.0": [-55, 0, 10], "0.3": [-160, 0, 30], "0.7": [-160, 0, 30], "1.0": [-55, 0, 10]}},
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.3": [-160, 0, -30], "0.7": [-160, 0, -30], "1.0": [0, 0, 0]}},
        "sunburst": {"scale": {"0.0": [1, 1, 1], "0.4": [1.5, 1.5, 1], "1.0": [1, 1, 1]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [-20, 0, 0], "1.0": [0, 0, 0]}},
    }
    return base_animations("virtue", extra_idle=extra, extra_move=extra, judge=judge, judge_len=1.0)
