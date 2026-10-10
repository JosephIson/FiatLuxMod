"""The Fallen - an angel cast out of heaven (Tier 2).

Isaiah 14:12: "How art thou fallen from heaven, O Lucifer, son of the morning!"
Luke 10:18: "I beheld Satan as lightning fall from heaven."
Revelation 12:9: "he was cast out into the earth, and his angels were cast out with him."
Jude 1:6: "the angels which kept not their first estate, but left their own habitation."

The Fall leaves its marks on the body: flesh scorched ash-grey where the lightning struck and
cracked with smouldering veins, the linen robe blackened and torn to ribbons, the girdle of gold
rusted crimson, the halo broken into drifting tarnished shards, eyes burning red - and the wings
torn away and mutated into skeletal bat wings of stretched membrane, ripped through, with a few
burnt feathers still clinging to the bone.
"""
import math

from geo import Model
from materials import *
from parts import hand, head, limb


def strip(m, bone, a, b, width, thick, mat):
    """A flat panel from a to b lying in the plane z = const (a and b share z): the box's long axis
    runs a->b and its width stays in-plane, so strips between neighbouring fingers tile a membrane."""
    ax, ay, az = a
    bx, by, bz = b
    dx, dy = bx - ax, by - ay
    L = math.hypot(dx, dy)
    c = ((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2)
    roll = math.degrees(math.atan2(dx, dy))
    return m.cube(bone, (c[0] - width / 2, c[1] - L / 2, c[2] - thick / 2), (width, L, thick), mat,
                  rotation=(0, 0, roll), pivot=c)


def lerp(p, q, t):
    return tuple(p[i] + (q[i] - p[i]) * t for i in range(3))


def bat_wing(m, side, parent, torn):
    """A torn skeletal bat wing in its own plane (z = Z), swept back by the root bone.
    Humerus and forearm, a clawed thumb, four finger struts, membranes between them and a large
    plagiopatagium back to the flank. `torn` = set of (membrane, strip) holes ripped through."""
    s = "l" if side > 0 else "r"
    X = lambda x: side * x
    Z = 3.4
    P = lambda x, y: (X(x), y, Z)
    root = m.bone(f"wing_{s}_root", (X(2.4), 27.0, 2.6), parent, rotation=(-8, side * -28, side * -6))
    m.centered(root, (X(2.4), 27.0, 3.0), (2.4, 2.6, 2.0), ASH_SKIN_SHADE, inflate=0.1)   # torn stump of the old wing
    m.centered(root, (X(2.6), 27.6, 3.2), (1.2, 1.2, 1.2), SCORCHED, rotation=(30, 0, 30), pivot=(X(2.6), 27.6, 3.2))

    shoulder, elbow, wrist = (2.6, 27.5), (9.5, 31.5), (18.0, 35.0)
    arm = m.bone(f"wing_{s}_arm", P(*shoulder), root)
    limb(m, arm, P(*shoulder), P(*elbow), 1.3, BONE_STRUT)
    m.centered(arm, P(*elbow), (1.7, 1.7, 1.7), BONE_DARK, rotation=(0, 0, 45), pivot=P(*elbow))
    fore = m.bone(f"wing_{s}_fore", P(*elbow), arm)
    limb(m, fore, P(*elbow), P(*wrist), 1.1, BONE_STRUT)
    m.centered(fore, P(*wrist), (1.6, 1.6, 1.6), BONE_DARK, rotation=(0, 0, 45), pivot=P(*wrist))
    # burnt remnants of the feathers that once grew here
    for k, (t, L) in enumerate(((0.25, 5.5), (0.5, 7.0), (0.72, 4.5), (0.15, 3.5))):
        if (k == 3) and side < 0:
            continue
        base = lerp(P(*elbow), P(*wrist), t) if k < 3 else lerp(P(*shoulder), P(*elbow), 0.6)
        tip = (base[0] + X(1.2), base[1] - L, Z + 0.4)
        limb(m, fore if k < 3 else arm, (base[0], base[1], Z + 0.4), tip, 0.7 if k % 2 else 0.9, BURNT_FEATHER)
    # the thumb: a hooked claw on the leading edge
    thumb_tip = P(20.5, 38.5)
    limb(m, fore, P(*wrist), thumb_tip, 0.7, BONE_STRUT)
    limb(m, fore, thumb_tip, P(20.0, 39.6), 0.45, BONE_DARK)

    fingers = [(31.0, 41.0), (35.5, 27.5), (32.0, 15.0), (25.5, 6.5)]
    tips = []
    fbones = []
    for k, (fx, fy) in enumerate(fingers):
        fb = m.bone(f"wing_{s}_finger_{k}", P(*wrist), fore)
        knuckle = lerp(P(*wrist), P(fx, fy), 0.5)
        limb(m, fb, P(*wrist), knuckle, 0.75, BONE_STRUT)
        limb(m, fb, knuckle, P(fx, fy), 0.55, BONE_STRUT)
        m.centered(fb, knuckle, (0.95, 0.95, 0.95), BONE_DARK)
        if k == 0:   # a claw at the leading tip
            limb(m, fb, P(fx, fy), P(fx + 1.2, fy + 1.0), 0.4, BONE_DARK)
        tips.append(P(fx, fy))
        fbones.append(fb)

    # membranes between the fingers (chiropatagium): strips across the triangle wrist-tip-tip
    for k in range(3):
        a_tip, b_tip = tips[k], tips[k + 1]
        n = 4
        for i in range(n):
            t = (i + 0.75) / n
            if (k, i) in torn:
                continue
            a = lerp(P(*wrist), a_tip, t)
            b = lerp(P(*wrist), b_tip, t)
            if i == n - 1:   # scalloped trailing edge sags between the tips
                b = lerp(P(*wrist), b_tip, t * (0.93 if (k + side) % 2 else 1.0))
            gap = math.dist(P(*wrist), a_tip) / n * 1.25
            strip(m, fbones[k], (a[0], a[1], Z), (b[0], b[1], Z), gap, 0.28, MEMBRANE if (i + k) % 2 else MEMBRANE_DARK)

    # plagiopatagium: from the arm (shoulder -> elbow -> wrist) back to a line flank -> last finger tip
    flank = P(3.0, 13.0)
    edge = [P(*shoulder), P(*elbow), P(*wrist)]
    def along_arm(t):
        if t < 0.45:
            return lerp(edge[0], edge[1], t / 0.45)
        return lerp(edge[1], edge[2], (t - 0.45) / 0.55)
    n = 5
    for i in range(n):
        t = (i + 0.5) / n
        if (3, i) in torn:
            continue
        a = along_arm(t)
        b = lerp(flank, tips[3], t)
        if (3, -i) in torn:          # ragged: this strip was ripped short
            b = lerp(a, b, 0.62)
        strip(m, fbones[3] if t > 0.6 else arm, (a[0], a[1], Z), (b[0], b[1], Z), 4.6, 0.28,
              MEMBRANE_DARK if i % 2 else MEMBRANE)
    return root


def build():
    m = Model("geometry.fiatlux.fallen", visible_bounds=(5, 4, (0, 1.4, 0)))
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 18, 0), "root")

    # the girdle that was gold of Uphaz, now rusted crimson, its ends torn
    m.cube("body", (-3.5, 17.0, -2.0), (7, 5, 4), TATTER)
    m.cube("body", (-3.7, 17.2, -2.2), (7.4, 1.8, 4.4), TATTER_TRIM, inflate=0.05)
    m.cube("body", (-1.3, 16.8, -2.8), (2.6, 2.4, 0.6), HALO_BROKEN)
    g = m.bone("girdle_end", (0.9, 17.0, -2.6), "body")
    m.cube(g, (0.4, 12.5, -2.8), (1.0, 4.5, 0.3), TATTER_TRIM, rotation=(-3, 0, 6), pivot=(0.9, 17.0, -2.6))

    # legs and clawed feet, scorched
    for side, s in ((1, "l"), (-1, "r")):
        lb = m.bone(f"leg_{s}", (side * 1.9, 17.0, 0), "body")
        x0 = side * 1.9 - 1.5
        m.cube(lb, (x0, 2.0, -1.5), (3, 15, 3), TATTER_SHADE)
        foot = m.bone(f"foot_{s}", (side * 1.9, 2.0, 0), lb)
        m.cube(foot, (x0, 0.0, -2.2), (3, 2, 3.6), SCORCHED)
        for k in range(3):   # talons
            tx = x0 + 0.35 + k * 0.95
            limb(m, foot, (tx + 0.15, 0.6, -2.2), (tx + 0.15 + (k - 1) * 0.3, 0.2, -3.6), 0.45, BONE_DARK)

    # the robe blackened and torn to ribbons: eight panels, each split into ragged strips
    m.bone("skirt", (0, 17.0, 0), "body")
    for k in range(8):
        a = 2 * math.pi * k / 8
        px, pz = math.sin(a) * 3.4, -math.cos(a) * 2.6
        yaw = -math.degrees(a)
        pb = m.bone(f"robe_panel_{k}", (px, 17.0, pz), "skirt")
        flare = 9
        rot = (flare * math.cos(a) * -1, yaw, flare * math.sin(a))
        for j, (dx, L) in enumerate(((-1.15, 15.0 - (k * 5 % 7)), (0.0, 11.0 + (k * 3 % 5)), (1.15, 13.5 - (k * 7 % 6)))):
            m.cube(pb, (px + dx - 0.6, 17.0 - L, pz - 0.3), (1.2, L, 0.6), TATTER if (j + k) % 2 else TATTER_SHADE,
                   rotation=(rot[0], rot[1], rot[2] + (j - 1) * 3), pivot=(px, 17.0, pz))

    # torso: what remains of the linen over scorched flesh, the chest torn open
    m.bone("torso", (0, 22.0, 0), "body")
    m.cube("torso", (-4.0, 21.8, -2.2), (8, 7, 4.4), SCORCHED)
    m.cube("torso", (-4.0, 21.8, -2.2), (3.4, 7, 4.4), TATTER, inflate=0.3)                     # torn mantle over one side
    m.cube("torso", (-4.0, 27.0, -2.2), (8, 1.8, 4.4), TATTER, inflate=0.3)                     # collar
    m.cube("torso", (1.0, 21.8, -2.6), (2.6, 2.0, 0.4), TATTER_SHADE, rotation=(0, 0, -14), pivot=(2.0, 22.8, -2.4))
    for k in range(4):   # ribs showing through the scorch
        m.cube("torso", (0.2, 23.2 + k * 1.1, -2.45), (3.2, 0.35, 0.3), ASH_SKIN_SHADE)
    for k, x in enumerate((-3.4, -1.6, 0.6, 2.6)):   # ribbons of the mantle down the back
        L = 14.0 + (k * 5 % 6)
        fb = m.bone(f"mantle_ribbon_{k}", (x + 0.5, 28.6, 2.4), "torso")
        m.cube(fb, (x, 28.6 - L, 2.2), (1.1, L, 0.5), TATTER_SHADE if k % 2 else TATTER,
               rotation=(8 + k, 0, (k - 1.5) * 4), pivot=(x + 0.5, 28.6, 2.4))

    # arms ending in clawed hands
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        arm = m.bone(f"arm_{s}", (X(5.0), 28.0, 0), "torso", rotation=(0, 0, side * -7))
        m.cube(arm, (lo(4.0, 7.0), 22.5, -1.5), (3, 6, 3), SCORCHED)
        m.cube(arm, (lo(3.9, 7.1), 26.4, -1.6), (3.2, 2.0, 3.2), TATTER, inflate=0.2)
        fore = m.bone(f"forearm_{s}", (X(5.5), 22.8, 0), arm)
        m.cube(fore, (lo(4.1, 6.9), 17.6, -1.4), (2.8, 5.2, 2.8), ASH_SKIN)
        m.cube(fore, (lo(4.0, 7.0), 19.6, -1.5), (3.0, 0.6, 3.0), SCORCHED, inflate=0.05)
        for k in range(2):   # bony spurs at the elbow
            limb(m, fore, (X(5.5), 22.2 - k * 0.9, 1.0), (X(6.2), 23.4 - k * 1.2, 2.6), 0.4, BONE_DARK)
        hand(m, f"hand_{s}", side, fore, (X(5.5), 17.6, 0), ASH_SKIN, ASH_SKIN_SHADE, nail=BONE_DARK, curl=28.0)
        bat_wing(m, side, "torso", torn={(1, 2), (3, -3)} if side > 0 else {(0, 1), (2, 3), (3, 4), (3, -1)})

    # head: the face scorched by the lightning that cast it down, eyes burning red
    m.bone("neck", (0, 29.0, 0), "torso")
    m.cube("neck", (-1.3, 28.8, -1.3), (2.6, 1.8, 2.6), ASH_SKIN_SHADE)
    head(m, "head", "neck", (0, 30.4, 0), ASH_SKIN, ASH_SKIN, ASH_SKIN_SHADE, ASH_SKIN_SHADE, RED_EYE)
    m.bone("hair", (0, 37.0, 0), "head")
    m.cube("hair", (-3.8, 36.3, -3.6), (7.6, 1.4, 7.4), HAIR_ASH)
    m.cube("hair", (-3.9, 32.0, 1.1), (7.8, 5.0, 2.8), HAIR_ASH)
    for k in range(7):   # long, wild, singed locks
        x = -3.3 + k * 1.1
        L = 7.0 + (k * 5 % 4) * 1.6
        hb = m.bone(f"hair_lock_{k}", (x + 0.5, 34.0, 3.6), "hair")
        m.cube(hb, (x, 34.0 - L, 3.3), (1.0, L, 0.9), HAIR_ASH, rotation=(12 + (k % 2) * 6, 0, (k - 3) * 4), pivot=(x + 0.5, 34.0, 3.7))

    # the halo, shattered: tarnished shards on a tilted broken ring, drifting apart
    m.bone("halo", (0, 41.0, 2.5), "head")
    R = 4.6
    shards = [(0, 2), (3, 4), (6, 7), (9, 9), (11, 12)]   # runs of the 14-segment ring that survive
    for k, (s0, s1) in enumerate(shards):
        sb = m.bone(f"halo_shard_{k}", (0, 41.0, 2.5), "halo")
        for seg in range(s0, s1 + 1):
            a = 2 * math.pi * (seg + 0.5) / 14
            px, py = math.cos(a) * R, 41.0 + math.sin(a) * R * 0.35
            pz = 2.5 + math.sin(a) * R * 0.9
            yaw = -math.degrees(a) + 90
            m.centered(sb, (px, py, pz), (2.1, 0.7, 0.7), HALO_BROKEN, rotation=(18, yaw, 0), pivot=(px, py, pz))
        a = 2 * math.pi * (s1 + 1) / 14     # the glowing crack where the shard broke off
        px, py, pz = math.cos(a) * R, 41.0 + math.sin(a) * R * 0.35, 2.5 + math.sin(a) * R * 0.9
        m.centered(sb, (px, py, pz), (0.5, 0.8, 0.8), HALO_CRACK, rotation=(18, -math.degrees(a) + 90, 0), pivot=(px, py, pz))
    return m


def animations():
    sin = lambda speed, phase=0, amp=1: f"math.sin(query.anim_time * {speed} + {phase}) * {amp}"
    WINGS = ("l", "r")

    def wing_pose(fold, flap_speed=None, flap_amp=0, phase=0):
        """fold: degrees the wing is swept in toward the back; flap adds an oscillation."""
        out = {}
        for s in WINGS:
            sg = 1 if s == "l" else -1
            flap = f" + {sin(flap_speed, phase, flap_amp)}" if flap_speed else ""
            out[f"wing_{s}_root"] = {"rotation": [sin(60, 0, 2), sg * fold, f"{sg} * (0{flap})"]}
            for k in range(4):
                out[f"wing_{s}_finger_{k}"] = {"rotation": [0, 0, f"{sg} * ({(3 - k) * 2} + {sin(flap_speed or 50, phase + 30 + k * 15, 2 + flap_amp * 0.15)})"]}
        return out

    idle = {
        "body": {"position": [0, sin(80, 0, 0.6), 0]},
        "torso": {"rotation": [f"6 + {sin(80, 40, 1.5)}", 0, 0]},          # a hunched, brooding stance
        "head": {"rotation": [f"8 + {sin(40, 20, 3)}", sin(25, 0, 10), sin(33, 0, 3)]},
        "arm_l": {"rotation": [sin(80, 30, 3), 0, sin(80, 0, 2)]},
        "arm_r": {"rotation": [sin(80, 210, 3), 0, sin(80, 180, 2)]},
        "halo": {"rotation": [sin(20, 0, 6), "query.anim_time * 18", sin(27, 90, 6)]},
        "head_eyelid_l": {"scale": {"0.0": [1, 1, 1], "2.9": [1, 1, 1], "3.0": [1, 3.4, 1], "3.1": [1, 1, 1]}},
        "head_eyelid_r": {"scale": {"0.0": [1, 1, 1], "2.9": [1, 1, 1], "3.0": [1, 3.4, 1], "3.1": [1, 1, 1]}},
    }
    idle.update(wing_pose(22, 50, 6))
    for k in range(5):   # the shards drift apart and back, never rejoining
        idle[f"halo_shard_{k}"] = {"position": [sin(45, k * 72, 0.6), sin(60, k * 72 + 30, 0.5), sin(50, k * 72 + 60, 0.6)],
                                   "rotation": [sin(35, k * 50, 8), 0, sin(41, k * 50 + 90, 8)]}
    for k in range(8):
        idle[f"robe_panel_{k}"] = {"rotation": [sin(70, k * 45, 3), 0, sin(55, k * 45 + 90, 2)]}
    for k in range(4):
        idle[f"mantle_ribbon_{k}"] = {"rotation": [sin(60, k * 70, 4), 0, sin(50, k * 60, 3)]}
    for k in range(7):
        idle[f"hair_lock_{k}"] = {"rotation": [sin(70, k * 40, 3), 0, sin(55, k * 50, 2)]}

    fly = {
        "body": {"rotation": [22, 0, 0], "position": [0, sin(360, 0, 1.0), 0]},
        "leg_l": {"rotation": [18, 0, 0]},
        "leg_r": {"rotation": [26, 0, 0]},
        "arm_l": {"rotation": [20, 0, 14]},
        "arm_r": {"rotation": [20, 0, -14]},
        "head": {"rotation": [-18, 0, 0]},
        "halo": {"rotation": [sin(20, 0, 6), "query.anim_time * 40", sin(27, 90, 6)]},
    }
    fly.update(wing_pose(0, 360, 34))
    for k in range(8):
        fly[f"robe_panel_{k}"] = {"rotation": [f"{24 if k in (3, 4, 5) else 10} + {sin(360, k * 45, 5)}", 0, sin(360, k * 45, 3)]}
    for k in range(4):
        fly[f"mantle_ribbon_{k}"] = {"rotation": [f"50 + {sin(360, k * 70, 8)}", 0, sin(300, k * 60, 4)]}
    for k in range(7):
        fly[f"hair_lock_{k}"] = {"rotation": [f"30 + {sin(360, k * 40, 5)}", 0, 0]}

    claw = {
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.2": [-150, -20, -30], "0.4": [20, 30, 10], "0.65": [0, 0, 0]}},
        "arm_l": {"rotation": {"0.0": [0, 0, 0], "0.25": [-40, 0, 20], "0.65": [0, 0, 0]}},
        "torso": {"rotation": {"0.0": [0, 0, 0], "0.2": [-8, 18, 0], "0.4": [16, -20, 0], "0.65": [0, 0, 0]}},
        "head_jaw": {"rotation": {"0.0": [0, 0, 0], "0.2": [22, 0, 0], "0.5": [0, 0, 0]}},   # a snarl
        "wing_l_root": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 22, -30], "0.65": [0, 0, 0]}},
        "wing_r_root": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, -22, 30], "0.65": [0, 0, 0]}},
    }

    # Luke 10:18 - the plunge: head-first, wings crumpled and flailing, limbs thrown up
    fall = {
        "body": {"rotation": [f"160 + {sin(400, 0, 12)}", sin(90, 0, 40), sin(300, 60, 10)], "position": [0, 10, 0]},
        "arm_l": {"rotation": [f"-150 + {sin(700, 0, 25)}", 0, f"40 + {sin(600, 30, 15)}"]},
        "arm_r": {"rotation": [f"-150 + {sin(700, 90, 25)}", 0, f"-40 + {sin(600, 120, 15)}"]},
        "leg_l": {"rotation": [sin(650, 0, 30), 0, 8]},
        "leg_r": {"rotation": [sin(650, 180, 30), 0, -8]},
        "head": {"rotation": [-30, 0, 0]},
        "head_jaw": {"rotation": [25, 0, 0]},                             # a cry as it falls
        "halo": {"position": [0, f"6 + {sin(300, 0, 2)}", 0], "rotation": [sin(500, 0, 40), "query.anim_time * 400", 0]},
    }
    for s, sg in (("l", 1), ("r", -1)):
        fall[f"wing_{s}_root"] = {"rotation": [f"-40 + {sin(900, 0 if s == 'l' else 140, 30)}", sg * 10, f"{sg} * (50 + {sin(800, 0, 25)})"]}
        for k in range(4):
            fall[f"wing_{s}_finger_{k}"] = {"rotation": [0, 0, f"{sg} * {sin(900, k * 40, 18)}"]}
    for k in range(8):
        fall[f"robe_panel_{k}"] = {"rotation": [f"-150 + {sin(800, k * 45, 15)}", 0, sin(700, k * 45, 10)]}
    for k in range(4):
        fall[f"mantle_ribbon_{k}"] = {"rotation": [f"-140 + {sin(800, k * 70, 20)}", 0, 0]}
    for k in range(5):
        fall[f"halo_shard_{k}"] = {"position": [sin(300, k * 72, 3), sin(300, k * 72 + 30, 3), sin(300, k * 72 + 60, 3)]}

    return {
        "animation.fallen.idle": {"loop": True, "animation_length": 6.0, "bones": idle},
        "animation.fallen.fly": {"loop": True, "animation_length": 1.0, "bones": fly},
        "animation.fallen.claw": {"loop": False, "animation_length": 0.65, "bones": claw},
        "animation.fallen.fall": {"loop": True, "animation_length": 2.0, "bones": fall},
    }


def preview_pose():
    """The fly pose at mid-flap, to check the wing sweep reads correctly."""
    return {"wing_l_root": (0, 0, 30), "wing_r_root": (0, 0, -30), "body": (22, 0, 0)}
