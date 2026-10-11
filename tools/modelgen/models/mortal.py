"""The Mortal - a man or woman of a created world (Tier 1, built well past it).

"So God created man in his own image ... male and female created he them" (Genesis 1:27).
One skeleton serves every mortal: the renderer shows the bones for the mortal's sex (a beard, or
long braided hair) and for the age its people have reached - the hunter's furs and spear, the
farmer's sickle, the classical toga and scroll, the medieval hood and hammer, the industrial coat
and lamp, the spacefarer's suit, visor and tablet. Texture variants give different skin tones,
hair colours and garments over the same geometry.
"""
import math

from geo import Material, Model
from materials import *
from parts import hand, head, limb
from figure import base_animations, sin

PALETTES = {
    # name: (skin, skin shade, hair, tunic, tunic shade)
    "a": ((236, 200, 170), (212, 174, 146), (70, 48, 30), (176, 150, 110), (150, 126, 90)),
    "b": ((198, 150, 110), (172, 126, 90), (30, 24, 20), (120, 96, 70), (100, 80, 58)),
    "c": ((140, 96, 66), (118, 80, 54), (24, 20, 18), (196, 172, 120), (170, 146, 100)),
    "d": ((96, 64, 44), (80, 52, 36), (20, 16, 14), (150, 70, 50), (126, 56, 40)),
    "e": ((226, 186, 150), (200, 160, 128), (214, 176, 96), (90, 110, 130), (74, 92, 110)),
    "f": ((244, 214, 190), (220, 188, 164), (170, 70, 40), (110, 130, 80), (90, 108, 64)),
    "g": ((176, 126, 88), (152, 106, 72), (60, 40, 26), (160, 120, 160), (136, 100, 136)),
    "h": ((214, 176, 140), (190, 152, 118), (150, 150, 150), (130, 120, 100), (110, 100, 84)),
}


def mats(key):
    skin, shade, hair, tunic, tunic_shade = PALETTES[key]
    return {
        "skin": Material(f"m_skin_{key}", skin, "skin"),
        "shade": Material(f"m_shade_{key}", shade, "skin"),
        "lips": Material(f"m_lips_{key}", tuple(int(c * 0.82) for c in shade), "skin"),
        "hair": Material(f"m_hair_{key}", hair, "hair"),
        "tunic": Material(f"m_tunic_{key}", tunic, "cloth", accent=tuple(min(255, int(c * 1.3)) for c in tunic)),
        "tunic_shade": Material(f"m_tunic_shade_{key}", tunic_shade, "cloth", accent=tunic),
    }


EYE = Material("m_eye", (60, 44, 30), "plain")
FUR = Material("m_fur", (120, 86, 52), "hair")
FUR_LIGHT = Material("m_fur_light", (186, 160, 120), "hair")
LEATHER_M = Material("m_leather", (110, 74, 44), "cloth")
WOOD_M = Material("m_wood", (126, 88, 52), "wood")
FLINT = Material("m_flint", (90, 90, 96), "plain")
IRON_M = Material("m_iron", (160, 164, 172), "metal")
TOGA = Material("m_toga", (240, 236, 222), "cloth", accent=(120, 40, 90))
TOGA_STRIPE = Material("m_toga_stripe", (110, 36, 86), "cloth")
PARCHMENT = Material("m_parchment", (226, 206, 160), "plain")
HOOD = Material("m_hood", (92, 70, 54), "cloth")
COAT = Material("m_coat", (54, 58, 70), "cloth", accent=(180, 150, 80))
BRASS = Material("m_brass", (200, 160, 70), "metal")
LAMP_GLOW = Material("m_lamp_glow", (255, 200, 110), "fire", emissive=True)
SUIT = Material("m_suit", (226, 230, 236), "metal", accent=(90, 150, 220))
SUIT_TRIM = Material("m_suit_trim", (70, 130, 210), "metal", emissive=True)
VISOR = Material("m_visor", (120, 190, 255), "crystal", alpha=130)
SCREEN = Material("m_screen", (110, 220, 255), "rune", emissive=True, accent=(220, 250, 255))
SANDAL = Material("m_sandal", (130, 92, 56), "cloth")
GOLD_M = Material("m_gold", (236, 190, 66), "gold", accent=(255, 236, 150))
RUBY = Material("m_ruby", (200, 30, 50), "crystal", emissive=True)
SAPPHIRE_M = Material("m_sapphire", (50, 90, 220), "crystal", emissive=True)
HOLD = 70.0   # the right forearm carries the tool of the age; tool bones cancel the bend so tools are modelled upright


def tool_bone(m, name):
    return m.bone(name, (-4.65, 12.9, -0.2), "hand_r", rotation=(HOLD, 0, 0))


def build(key="a"):
    p = mats(key)
    m = Model("geometry.fiatlux.mortal", visible_bounds=(2.5, 2.8, (0, 1.2, 0)))
    skin, shade, hair, tunic, tshade = p["skin"], p["shade"], p["hair"], p["tunic"], p["tunic_shade"]
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 14, 0), "root")
    # hips and belt
    m.cube("body", (-3.2, 13.0, -1.8), (6.4, 4.0, 3.6), tunic)
    m.cube("body", (-3.35, 13.4, -1.95), (6.7, 0.9, 3.9), LEATHER_M, inflate=0.05)
    # legs, feet with sandals
    for side, s in ((1, "l"), (-1, "r")):
        lb = m.bone(f"leg_{s}", (side * 1.6, 13.0, 0), "body")
        x0 = side * 1.6 - 1.35
        m.cube(lb, (x0, 6.5, -1.35), (2.7, 6.5, 2.7), skin)
        knee = m.bone(f"shin_{s}", (side * 1.6, 6.5, 0), lb)
        m.cube(knee, (x0 + 0.05, 1.0, -1.3), (2.6, 5.5, 2.6), skin)
        foot = m.bone(f"foot_{s}", (side * 1.6, 1.0, 0), knee)
        m.cube(foot, (x0, 0.0, -2.3), (2.7, 1.2, 3.8), skin)
        m.cube(foot, (x0 - 0.05, 0.0, -2.35), (2.8, 0.4, 3.9), SANDAL)
        m.cube(foot, (x0 + 1.15, 0.4, -2.2), (0.4, 0.9, 2.2), SANDAL)
    # tunic skirt: six boned panels
    m.bone("skirt", (0, 13.4, 0), "body")
    for k in range(6):
        a = 2 * math.pi * k / 6
        px, pz = math.sin(a) * 3.0, -math.cos(a) * 2.0
        pb = m.bone(f"skirt_{k}", (px, 13.4, pz), "skirt")
        rot = (-8 * math.cos(a), -math.degrees(a), 8 * math.sin(a))
        m.cube(pb, (px - 1.6, 7.2, pz - 0.3), (3.2, 6.2, 0.6), tunic if k % 2 == 0 else tshade, rotation=rot, pivot=(px, 13.4, pz))
    # torso
    m.bone("torso", (0, 17.0, 0), "body")
    m.cube("torso", (-3.4, 16.8, -1.9), (6.8, 6.4, 3.8), tunic)
    m.cube("torso", (-3.4, 16.8, -1.9), (6.8, 6.4, 3.8), tunic, inflate=0.2)
    m.cube("torso", (-1.2, 21.4, -2.2), (2.4, 1.8, 0.4), shade)       # neckline
    for k in range(3):
        m.cube("torso", (-1.8 + k * 1.6, 17.2, -2.15), (0.3, 4.0, 0.2), tshade)   # folds
    # arms with hands
    for side, s in ((1, "l"), (-1, "r")):
        X = lambda x: side * x
        lo = lambda a, b: min(X(a), X(b))
        arm = m.bone(f"arm_{s}", (X(4.2), 22.6, 0), "torso", rotation=(0, 0, side * -4))
        m.cube(arm, (lo(3.4, 5.9), 18.4, -1.25), (2.5, 4.6, 2.5), tunic, inflate=0.15)
        fore = m.bone(f"forearm_{s}", (X(4.65), 18.5, 0), arm, rotation=(-HOLD if s == "r" else 0, 0, 0))
        m.cube(fore, (lo(3.55, 5.75), 14.5, -1.1), (2.2, 4.1, 2.2), skin)
        hand(m, f"hand_{s}", side, fore, (X(4.65), 14.5, 0), skin, shade, nail=shade, curl=30.0)
    # head
    m.bone("neck", (0, 23.2, 0), "torso")
    m.cube("neck", (-1.1, 23.0, -1.1), (2.2, 1.6, 2.2), skin)
    head(m, "head", "neck", (0, 24.4, 0), skin, skin, shade, p["lips"], EYE, hair=hair)
    m.bone("hair", (0, 31.0, 0), "head")
    m.cube("hair", (-3.6, 28.6, 1.6), (7.2, 2.6, 2.0), hair)
    # a man's beard
    bd = m.bone("beard", (0, 25.6, -3.2), "head")
    m.cube(bd, (-2.4, 23.8, -3.9), (4.8, 2.2, 1.2), hair)
    m.cube(bd, (-1.6, 23.0, -3.6), (3.2, 1.2, 1.0), hair)
    m.cube(bd, (-3.0, 25.2, -3.4), (0.8, 1.8, 1.6), hair)
    m.cube(bd, (2.2, 25.2, -3.4), (0.8, 1.8, 1.6), hair)
    # a woman's long hair: back fall and a braid
    lh = m.bone("long_hair", (0, 30.0, 3.6), "head")
    m.cube(lh, (-3.7, 21.0, 2.6), (7.4, 9.0, 1.2), hair, rotation=(6, 0, 0), pivot=(0, 30.0, 3.6))
    for k in range(4):
        m.cube(lh, (2.9, 26.0 - k * 1.7, -1.0), (1.0, 1.6, 1.0), hair, rotation=(0, 0, 10), pivot=(3.4, 26.8 - k * 1.7, -0.5))
    for side in (1, -1):
        m.cube(lh, (side * 3.6 - (0 if side > 0 else 0.7), 24.0, -2.6), (0.7, 6.0, 2.4), hair)

    # a ruler's crown: a gold circlet with points and set stones (shown for kings, queens and emperors)
    cr = m.bone("crown", (0, 31.0, 0), "head")
    m.ring(cr, (0, 30.9, 0), 3.9, 12, (2.1, 1.0, 0.4), GOLD_M)
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = math.cos(a) * 3.9, math.sin(a) * 3.9
        m.cube(cr, (x - 0.35, 31.3, z - 0.35), (0.7, 1.3, 0.7), GOLD_M)
        m.cube(cr, (x - 0.3, 30.75, z - 0.3), (0.6, 0.6, 0.6), RUBY if k % 2 else SAPPHIRE_M)

    # --- dress and tools of each age (the renderer shows only the bones of the mortal's age)
    # hunter-gatherer: fur pelt over one shoulder, flint spear
    hg = m.bone("era_hunter", (0, 17.0, 0), "torso")
    m.cube(hg, (-3.7, 17.5, -2.3), (7.4, 5.6, 4.6), FUR, rotation=(0, 0, -14), pivot=(0, 20, 0))
    m.cube(hg, (-3.9, 21.6, -2.4), (3.4, 1.4, 4.8), FUR_LIGHT, rotation=(0, 0, -14), pivot=(0, 20, 0))
    m.cube(hg, (-3.4, 11.6, -2.0), (6.8, 2.6, 4.0), FUR)
    sp = tool_bone(m, "tool_hunter")
    limb(m, sp, (-4.65, 3.0, -0.2), (-4.65, 26.0, -0.2), 0.5, WOOD_M)
    limb(m, sp, (-4.65, 26.0, -0.2), (-4.65, 28.6, -0.2), 0.9, FLINT)
    # agrarian: straw hat and sickle
    ag = m.bone("era_agrarian", (0, 31.0, 0), "head")
    m.cube(ag, (-5.0, 30.6, -5.0), (10.0, 0.6, 10.0), Material("m_straw", (220, 196, 120), "cloth"))
    m.cube(ag, (-3.2, 31.2, -3.2), (6.4, 1.8, 6.4), Material("m_straw2", (206, 180, 104), "cloth"))
    sk = tool_bone(m, "tool_agrarian")
    limb(m, sk, (-4.65, 12.0, -0.2), (-4.65, 16.5, -0.2), 0.5, WOOD_M)
    limb(m, sk, (-4.65, 16.5, -0.2), (-4.65, 18.0, -2.4), 0.35, IRON_M)
    limb(m, sk, (-4.65, 18.0, -2.4), (-4.65, 16.8, -4.0), 0.35, IRON_M)
    # classical: toga draped over the shoulder, and a scroll
    cl = m.bone("era_classical", (0, 17.0, 0), "torso")
    m.cube(cl, (-3.7, 15.5, -2.3), (7.4, 8.0, 4.6), TOGA, rotation=(0, 0, 18), pivot=(0, 20, 0))
    m.cube(cl, (0.5, 16.0, -2.5), (1.0, 7.2, 0.3), TOGA_STRIPE, rotation=(0, 0, 18), pivot=(0, 20, 0))
    m.cube(cl, (-3.5, 7.0, -2.2), (7.0, 7.0, 4.4), TOGA)
    sc = tool_bone(m, "tool_classical")
    m.cube(sc, (-5.3, 12.2, -1.7), (1.3, 1.3, 3.2), PARCHMENT)
    m.cube(sc, (-5.4, 12.1, -1.9), (1.5, 1.5, 0.4), WOOD_M)
    m.cube(sc, (-5.4, 12.1, 1.3), (1.5, 1.5, 0.4), WOOD_M)
    # medieval: hood and mantle, smith's hammer
    md = m.bone("era_medieval", (0, 31.0, 0), "head")
    m.cube(md, (-4.0, 26.4, -3.2), (8.0, 5.4, 7.4), HOOD, inflate=0.2)
    m.cube(md, (-4.2, 22.0, -1.0), (8.4, 4.6, 5.4), HOOD, rotation=(10, 0, 0), pivot=(0, 26, 2))
    hm = tool_bone(m, "tool_medieval")
    limb(m, hm, (-4.65, 11.5, -0.2), (-4.65, 18.5, -0.2), 0.5, WOOD_M)
    m.cube(hm, (-5.4, 18.2, -1.6), (1.5, 1.5, 3.2), IRON_M)
    # industrial: long coat, waistcoat chain, and a lamp
    ind = m.bone("era_industrial", (0, 17.0, 0), "torso")
    m.cube(ind, (-3.7, 16.6, -2.2), (7.4, 6.8, 4.4), COAT)
    m.cube(ind, (-3.6, 7.6, -2.1), (7.2, 9.0, 4.2), COAT, rotation=(4, 0, 0), pivot=(0, 16.6, 0))
    m.cube(ind, (-1.6, 18.5, -2.45), (3.2, 0.25, 0.2), BRASS)
    lp = tool_bone(m, "tool_industrial")
    m.cube(lp, (-5.2, 9.0, -0.75), (1.4, 0.4, 1.4), BRASS)
    m.cube(lp, (-5.1, 9.4, -0.65), (1.2, 1.8, 1.2), LAMP_GLOW)
    m.cube(lp, (-5.2, 11.2, -0.75), (1.4, 0.4, 1.4), BRASS)
    limb(m, lp, (-4.5, 11.6, -0.05), (-4.65, 12.9, -0.2), 0.2, BRASS)
    # spacefaring: suit, helmet with visor, and a glowing tablet
    sf = m.bone("era_spacefaring", (0, 17.0, 0), "torso")
    m.cube(sf, (-3.6, 16.6, -2.1), (7.2, 6.8, 4.2), SUIT, inflate=0.15)
    m.cube(sf, (-1.4, 19.0, -2.45), (2.8, 2.0, 0.3), SUIT_TRIM)
    m.cube(sf, (-2.6, 17.0, 2.0), (5.2, 5.4, 1.8), SUIT)
    hl = m.bone("era_spacefaring_helm", (0, 31.0, 0), "head")
    m.cube(hl, (-4.2, 24.0, -4.0), (8.4, 8.4, 8.0), SUIT, inflate=0.1)
    m.cube(hl, (-3.4, 25.2, -4.35), (6.8, 4.6, 0.4), VISOR)
    tb = tool_bone(m, "tool_spacefaring")
    m.cube(tb, (-5.3, 12.2, -2.6), (1.0, 2.6, 3.4), SUIT)
    m.cube(tb, (-5.45, 12.4, -2.4), (0.2, 2.2, 3.0), SCREEN)
    return m


def variants():
    """Same geometry, different skin, hair and garments: one texture per palette."""
    return {k: build(k) for k in PALETTES if k != "a"}


# Held poses that props (the cross, the stocks) are built to fit: see pose_check.py.
POSES = {
    # hung upon a cross: arms out along the beam, palms forward, legs together with the knees a little bent
    # and the feet pointing down onto the footrest, the head bowed to one side
    "crucified": {
        "arm_l": (0, 0, -86), "arm_r": (0, 0, 86),
        "forearm_l": (0, 0, 0), "forearm_r": (HOLD, 0, 0),
        "head": (26, 14, 0), "torso": (2, 0, 0),
        "leg_l": (-7, 0, -1.5), "leg_r": (-7, 0, 1.5),
        "shin_l": (14, 0, 0), "shin_r": (14, 0, 0),
        "foot_l": (24, 0, 0), "foot_r": (24, 0, 0),
    },
    # in the stocks: bent at the waist, head and wrists held in the holes of the board
    "stocks": {
        "torso": (62, 0, 0), "neck": (-10, 0, 0), "head": (-48, 0, 0),
        "arm_l": (-106, -24, 32), "arm_r": (-106, 24, -32),
        "forearm_l": (-140, 0, 0), "forearm_r": (HOLD - 140, 0, 0),
        "leg_l": (-4, 0, 0), "leg_r": (-4, 0, 0),
    },
}


def pose(name):
    return POSES[name]


def animations():
    extra = {"skirt": {"rotation": [sin(70, 0, 2), 0, 0]}}
    walk = {
        "leg_l": {"rotation": [sin(300, 0, 28), 0, 0]},
        "leg_r": {"rotation": [sin(300, 180, 28), 0, 0]},
        "shin_l": {"rotation": [f"math.max(0, {sin(300, 90, 30)})", 0, 0]},
        "shin_r": {"rotation": [f"math.max(0, {sin(300, 270, 30)})", 0, 0]},
        "arm_l": {"rotation": [sin(300, 180, 22), 0, 0]},
        "arm_r": {"rotation": [sin(300, 0, 12), 0, 0]},
        "body": {"position": [0, f"math.abs({sin(300, 0, 0.6)})", 0]},
    }
    for k in range(6):
        walk[f"skirt_{k}"] = {"rotation": [sin(300, k * 60, 5), 0, 0]}
    pray = {   # kneeling, hands lifted toward heaven
        "body": {"position": [0, -5.5, 0], "rotation": [0, 0, 0]},
        "leg_l": {"rotation": [-90, 0, 0]},
        "leg_r": {"rotation": [-90, 0, 0]},
        "shin_l": {"rotation": [90, 0, 0]},
        "shin_r": {"rotation": [90, 0, 0]},
        "arm_l": {"rotation": [f"-150 + {sin(60, 0, 4)}", 0, 18]},
        "arm_r": {"rotation": [f"-150 + {sin(60, 90, 4)}", 0, -18]},
        "head": {"rotation": [-25, 0, 0]},
    }
    work = {
        "arm_r": {"rotation": [f"-60 + {sin(400, 0, 45)}", 0, 0]},
        "torso": {"rotation": [f"8 + {sin(400, 0, 5)}", 0, 0]},
        "head": {"rotation": [18, 0, 0]},
    }
    anims = base_animations("mortal", primaries=0, extra_idle=extra, extra_move=walk, judge=work, judge_len=1.0)
    anims["animation.mortal.idle"]["bones"].pop("arm_r", None)
    anims["animation.mortal.move"]["bones"] = {k: v for k, v in anims["animation.mortal.move"]["bones"].items()
                                                if not k.startswith(("robe_panel", "mantle_fold", "wing_"))}
    anims["animation.mortal.move"]["bones"].update(walk)
    anims["animation.mortal.move"]["animation_length"] = 1.2
    anims["animation.mortal.pray"] = {"loop": True, "animation_length": 6.0, "bones": pray}
    # asleep: lying on one's back, breathing slowly
    anims["animation.mortal.sleep"] = {"loop": True, "animation_length": 4.0, "bones": {
        "root": {"rotation": [-90, 0, 0], "position": [0, 2, -14]},
        "torso": {"rotation": [sin(90, 0, 1.5), 0, 0]},
        "arm_l": {"rotation": [0, 0, 8]}, "arm_r": {"rotation": [0, 0, -8]},
        "head_eyelid_l": {"scale": [1, 3.4, 1]}, "head_eyelid_r": {"scale": [1, 3.4, 1]},
    }}
    # eating at the hearth: bread to the mouth
    anims["animation.mortal.eat"] = {"loop": True, "animation_length": 1.5, "bones": {
        "arm_l": {"rotation": [f"-95 + {sin(480, 0, 12)}", 25, 0]},
        "forearm_l": {"rotation": [-40, 0, 0]},
        "head": {"rotation": [f"10 + {sin(480, 30, 6)}", 0, 0]},
        "head_jaw": {"rotation": [f"math.max(0, {sin(960, 0, 10)})", 0, 0]},
    }}
    # talking: gestures and nods
    anims["animation.mortal.talk"] = {"loop": True, "animation_length": 3.0, "bones": {
        "arm_l": {"rotation": [f"-30 + {sin(240, 0, 18)}", 0, f"10 + {sin(170, 40, 10)}"]},
        "head": {"rotation": [sin(200, 0, 6), sin(110, 20, 10), 0]},
        "head_jaw": {"rotation": [f"math.max(0, {sin(900, 0, 8)})", 0, 0]},
        "torso": {"rotation": [sin(120, 0, 2), sin(90, 0, 4), 0]},
    }}
    # a bow of reverence
    anims["animation.mortal.bow"] = {"loop": True, "animation_length": 2.0, "bones": {
        "torso": {"rotation": [45, 0, 0]}, "head": {"rotation": [20, 0, 0]},
        "arm_l": {"rotation": [-20, 0, 10]}, "arm_r": {"rotation": [-20, 0, -10]},
    }}
    # rejoicing: arms flung up, a little hop
    anims["animation.mortal.cheer"] = {"loop": True, "animation_length": 1.0, "bones": {
        "arm_l": {"rotation": [f"-160 + {sin(720, 0, 15)}", 0, 20]},
        "arm_r": {"rotation": [f"-160 + {sin(720, 90, 15)}", 0, -20]},
        "body": {"position": [0, f"math.abs({sin(360, 0, 1.6)})", 0]},
        "head": {"rotation": [-15, 0, 0]},
    }}
    # condemned: hung upon a cross (the pose the cross model is built to fit), breathing shallowly
    cr = {b: {"rotation": list(r)} for b, r in POSES["crucified"].items()}
    cr["torso"] = {"rotation": [f"2 + {sin(30, 0, 1.2)}", 0, 0]}
    cr["head"] = {"rotation": [f"26 + {sin(30, 40, 2)}", 14, 0]}
    anims["animation.mortal.crucified"] = {"loop": True, "animation_length": 6.0, "bones": cr}
    # in the stocks (the pose the stocks model is built to fit)
    st = {b: {"rotation": list(r)} for b, r in POSES["stocks"].items()}
    anims["animation.mortal.stocks"] = {"loop": True, "animation_length": 4.0, "bones": st}
    # casting out an unclean spirit: a hand raised toward it
    anims["animation.mortal.exorcise"] = {"loop": True, "animation_length": 2.0, "bones": {
        "arm_r": {"rotation": [f"-110 + {sin(180, 0, 6)}", 0, -10]}, "forearm_r": {"rotation": [HOLD, 0, 0]},
        "arm_l": {"rotation": [-30, 0, 20]}, "head": {"rotation": [-5, 0, 0]},
        "torso": {"rotation": [-4, 0, 0]},
    }}
    anims["animation.mortal.work"] = anims.pop("animation.mortal.judge")
    anims["animation.mortal.work"]["loop"] = True
    for a in anims.values():
        a["bones"] = {k: v for k, v in a["bones"].items() if not k.startswith(("robe_panel", "mantle_fold", "wing_", "hair_back"))}
    return anims
