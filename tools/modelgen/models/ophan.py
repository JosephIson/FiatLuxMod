"""Ophan - the wheels beside the living creatures (Tier 3).

Ezekiel 1:15-21 (KJV): "behold one wheel upon the earth by the living creatures ... The
appearance of the wheels and their work was like unto the colour of a beryl ... their work was
as it were a wheel in the middle of a wheel. When they went, they went upon their four sides:
and they turned not when they went. As for their rings, they were so high that they were
dreadful; and their rings were full of eyes round about them four ... for the spirit of the
living creature was in the wheels."  Ezekiel 10:12-13: "the wheels, were full of eyes round
about ... it was cried unto them in my hearing, O wheel."

Two beryl wheels at right angles (one rolls forward/back about x, the other sideways about z),
so the Ophan can travel in any of four directions without turning. Each wheel has a high outer
rim, a glowing inner rim and slender felloe struts; both faces of every rim are set with eyes
that blink on their own rhythm. At the centre burns the spirit: a living core of fire.
"""
import math

from geo import Model
from materials import *
from parts import eye

R_OUTER = 15.0
R_INNER = 12.6
EYES_PER_FACE = 16


def _wheel(m, name, axis, radius, center):
    """axis 'x': wheel lies in the y-z plane (rolls forward); axis 'z': x-y plane (rolls sideways)."""
    cx, cy, cz = center
    w = m.bone(name, center, "root")
    n = 32
    seg = 2 * math.pi * radius / n * 1.12
    if axis == "x":
        m.ring(w, center, radius, n, (3.4, seg, 3.2), BERYL, axis="x")
        m.ring(w, center, radius + 1.25, n, (1.4, seg * 1.08, 0.5), UPHAZ_GOLD, axis="x")         # gilded tyre
        m.ring(w, center, R_INNER * radius / R_OUTER, n, (2.0, seg * 0.85, 0.9), BERYL_GLOW, axis="x", phase=0.1)
    else:
        m.ring(w, center, radius, n, (seg, 3.2, 3.4), BERYL, axis="z")
        m.ring(w, center, radius + 1.25, n, (seg * 1.08, 0.5, 1.4), UPHAZ_GOLD, axis="z")
        m.ring(w, center, R_INNER * radius / R_OUTER, n, (seg * 0.85, 0.9, 2.0), BERYL_GLOW, axis="z", phase=0.1)
    # felloe struts tying the rims together (not spokes - the hub is the burning spirit)
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        mid = (radius + R_INNER * radius / R_OUTER) / 2
        if axis == "x":
            p = (cx, cy + mid * math.cos(a), cz + mid * math.sin(a))
            m.centered(w, p, (3.0, 0.9, 0.9), UPHAZ_GOLD, rotation=(-math.degrees(a), 0, 0), pivot=p)
        else:
            p = (cx + mid * math.cos(a), cy + mid * math.sin(a), cz)
            m.centered(w, p, (0.9, 0.9, 3.0), UPHAZ_GOLD, rotation=(0, 0, -math.degrees(a)), pivot=p)
    # "their rings were full of eyes round about": both faces of the high rim
    for face in (1, -1):
        for k in range(EYES_PER_FACE):
            a = 2 * math.pi * (k + (0.5 if face < 0 else 0)) / EYES_PER_FACE
            r = radius - 0.2
            if axis == "x":
                pos = (cx + face * 1.85, cy + r * math.cos(a), cz + r * math.sin(a))
                normal = "+x" if face > 0 else "-x"
            else:
                pos = (cx + r * math.cos(a), cy + r * math.sin(a), cz + face * 1.85)
                normal = "+z" if face > 0 else "-z"
            eye(m, f"{name}_eye_{'p' if face > 0 else 'n'}{k}", w, pos, normal, size=1.45,
                iris=EYE_IRIS_GOLD if k % 3 else EYE_IRIS)
    return w


def build():
    m = Model("geometry.fiatlux.ophan", visible_bounds=(5, 5, (0, 1.2, 0)))
    center = (0, 18.0, 0)
    m.bone("root", (0, 0, 0))
    _wheel(m, "wheel_roll", "x", R_OUTER, center)          # rolls forward and back
    _wheel(m, "wheel_turn", "z", R_OUTER - 1.2, center)    # the wheel in the middle of the wheel

    # the spirit of the living creature in the wheels: nested, counter-rotating fire
    core = m.bone("spirit", center, "root")
    m.centered(core, center, (3.6, 3.6, 3.6), HOLY_FIRE_WHITE, rotation=(45, 45, 0), pivot=center)
    m.centered(core, center, (2.6, 2.6, 2.6), BERYL_GLOW, inflate=0.6)
    inner = m.bone("spirit_inner", center, "spirit")
    for k in range(6):
        ang = k * 30
        m.centered(inner, center, (5.4, 0.7, 0.7), HOLY_FIRE if k % 2 else HOLY_FIRE_WHITE,
                   rotation=(ang, ang * 1.5, ang * 0.5), pivot=center)
    embers = m.bone("embers", center, "root")
    for k in range(8):  # coals of fire between the wheels (Ezek 10:2)
        a = 2 * math.pi * k / 8
        p = (center[0] + 5.4 * math.cos(a), center[1] + 2.0 * math.sin(a * 2), center[2] + 5.4 * math.sin(a))
        m.centered(embers, p, (0.9, 0.9, 0.9), EMBER, rotation=(k * 20, k * 35, 0), pivot=p)
    return m


def animations():
    sin = lambda speed, phase=0, amp=1: f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    def blinks(prefix_list):
        out = {}
        for idx, e in enumerate(prefix_list):
            t = round(0.3 + (idx * 0.61) % 5.4, 2)
            out[f"{e}_lid"] = {"scale": {"0.0": [1, 1, 1], str(t): [1, 1, 1],
                                         str(round(t + 0.08, 2)): [1, 3.3, 1],
                                         str(round(t + 0.16, 2)): [1, 1, 1], "6.0": [1, 1, 1]}}
        return out
    eyes = [f"{w}_eye_{f}{k}" for w in ("wheel_roll", "wheel_turn") for f in ("p", "n") for k in range(EYES_PER_FACE)]
    idle = {
        "root": {"position": [0, sin(60, 0, 0.8), 0]},
        "wheel_roll": {"rotation": ["query.anim_time * 20", 0, 0]},
        "wheel_turn": {"rotation": [0, 0, "query.anim_time * -14"]},
        "spirit": {"rotation": ["query.anim_time * 40", "query.anim_time * 65", 0],
                   "scale": [f"1 + {sin(240, 0, 0.12)}"] * 3},
        "spirit_inner": {"rotation": [0, "query.anim_time * -150", "query.anim_time * 90"]},
        "embers": {"rotation": [0, "query.anim_time * 30", 0]},
    }
    idle.update(blinks(eyes))
    # "O wheel" - whirling at full speed when the creatures go (Ezek 10:13)
    whirl = dict(idle)
    whirl["wheel_roll"] = {"rotation": ["query.anim_time * 240", 0, 0]}
    whirl["wheel_turn"] = {"rotation": [0, 0, "query.anim_time * -180"]}
    whirl["spirit"] = {"rotation": ["query.anim_time * 160", "query.anim_time * 260", 0],
                       "scale": [f"1.2 + {sin(720, 0, 0.2)}"] * 3}
    return {
        "animation.ophan.idle": {"loop": True, "animation_length": 6.0, "bones": idle},
        "animation.ophan.whirl": {"loop": True, "animation_length": 6.0, "bones": whirl},
    }
