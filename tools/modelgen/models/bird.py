"""The birds of the ark (Tier 1 creatures): the dove, and (as a variant palette) the raven.

Genesis 8:7: "he sent forth a raven, which went forth to and fro"; 8:11: "the dove came in to him in
the evening; and, lo, in her mouth was an olive leaf pluckt off."

A small bird in flight: a rounded body and breast, a head with a short beak, wings of three feather
layers that beat from the shoulder, a fanned tail, tucked legs, and an olive sprig held in the beak
(shown only once the dove has plucked it). Faces -z.
"""
from geo import Material, Model
from parts import limb
from figure import sin

PALETTES = {
    "a": {   # the dove
        "body": (226, 226, 222), "shade": (188, 190, 196), "tip": (150, 152, 160), "beak": (196, 132, 128), "eye": (40, 26, 22),
        "leg": (200, 110, 110),
    },
    "raven": {
        "body": (34, 34, 44), "shade": (24, 26, 36), "tip": (52, 58, 82), "beak": (30, 30, 34), "eye": (16, 14, 14),
        "leg": (36, 34, 34),
    },
}
LEAF = Material("bd_leaf", (92, 128, 60), "plain", accent=(140, 168, 96))
STEM = Material("bd_stem", (110, 90, 60), "wood")


def build(palette="a"):
    c = PALETTES[palette]
    BODY = Material("bd_body", c["body"], "feather", accent=c["shade"])
    SHADE = Material("bd_shade", c["shade"], "feather", accent=c["tip"])
    TIP = Material("bd_tip", c["tip"], "feather", accent=c["shade"])
    BEAK = Material("bd_beak", c["beak"], "plain")
    EYE = Material("bd_eye", c["eye"], "plain")
    LEG = Material("bd_leg", c["leg"], "plain")

    m = Model("geometry.fiatlux.bird", visible_bounds=(2, 1.5, (0, 0.25, 0)), density=4)
    m.bone("root", (0, 0, 0))
    body = m.bone("body", (0, 4.0, 0), "root")
    m.centered(body, (0, 4.0, 0.4), (3.2, 2.8, 5.6), BODY)
    m.centered(body, (0, 3.7, -2.0), (2.8, 2.6, 2.2), BODY)              # the breast
    m.centered(body, (0, 5.25, 0.6), (2.4, 0.5, 4.6), SHADE)             # the back
    head = m.bone("head", (0, 5.0, -2.8), "body")
    m.centered(head, (0, 5.6, -3.6), (2.2, 2.2, 2.4), BODY)
    m.centered(head, (0, 6.55, -3.4), (1.8, 0.4, 1.8), SHADE)            # crown
    for side in (1, -1):
        m.centered(head, (side * 1.12, 5.9, -4.1), (0.15, 0.55, 0.55), EYE)
    beak = m.bone("beak", (0, 5.3, -4.8), "head")
    m.centered(beak, (0, 5.4, -5.4), (0.9, 0.7, 1.4), BEAK)
    m.centered(beak, (0, 5.15, -6.2), (0.55, 0.45, 0.6), BEAK)
    olive = m.bone("olive", (0, 5.2, -6.2), "beak")
    limb(m, olive, (-0.2, 5.2, -6.1), (1.6, 4.9, -6.8), 0.25, STEM)
    m.cube(olive, (0.3, 4.6, -7.4), (1.6, 0.15, 0.7), LEAF, rotation=(0, 20, -15), pivot=(0.3, 4.7, -7.0))
    m.cube(olive, (0.9, 4.95, -6.5), (1.4, 0.15, 0.6), LEAF, rotation=(0, -25, 10), pivot=(0.9, 5.0, -6.2))
    m.cube(olive, (-0.4, 4.85, -7.0), (1.1, 0.15, 0.5), LEAF, rotation=(0, 40, 5), pivot=(-0.4, 4.9, -6.8))
    # the wings: three layers of feathers, from the shoulder outward (+x is the bird's right)
    for side, name in ((1, "wing_r"), (-1, "wing_l")):
        w = m.bone(name, (side * 1.5, 4.9, -1.2), "body")
        x0 = side * 1.5
        def slab(x_in, x_out, z0, depth, mat, y=4.75, h=0.35):
            a, b = sorted((side * x_in, side * x_out))
            m.cube(w, (a, y, z0), (b - a, h, depth), mat)
        slab(1.5, 4.2, -1.9, 3.4, BODY, h=0.45)       # coverts
        slab(1.5, 6.4, -1.0, 4.2, SHADE)              # secondaries
        slab(4.0, 8.2, -0.6, 3.2, TIP, y=4.7, h=0.3)  # primaries
        tip = m.bone(name + "_tip", (side * 6.0, 4.85, 0.0), name)
        a, b = sorted((side * 6.0, side * 9.6))
        m.cube(tip, (a, 4.7, -0.3), (b - a, 0.28, 2.4), TIP)
    tail = m.bone("tail", (0, 4.4, 3.0), "body")
    m.cube(tail, (-1.3, 4.2, 3.0), (2.6, 0.4, 3.6), SHADE)
    m.cube(tail, (-0.9, 4.15, 6.2), (1.8, 0.35, 0.9), TIP)
    for side, name in ((1, "leg_r"), (-1, "leg_l")):
        leg = m.bone(name, (side * 0.7, 2.8, 0.6), "body")
        limb(m, leg, (side * 0.7, 2.8, 0.6), (side * 0.7, 2.0, 1.6), 0.35, LEG)
        m.centered(leg, (side * 0.7, 1.95, 1.9), (0.7, 0.25, 0.8), LEG)
    return m


def variants():
    return {k: build(k) for k in PALETTES if k != "a"}


def animations():
    flap = {
        "wing_r": {"rotation": [0, 0, f"20 + {sin(1100, 0, 48)}"]},
        "wing_l": {"rotation": [0, 0, f"-20 - {sin(1100, 0, 48)}"]},
        "wing_r_tip": {"rotation": [0, 0, f"{sin(1100, -40, 22)}"]},
        "wing_l_tip": {"rotation": [0, 0, f"-{sin(1100, -40, 22)}"]},
        "body": {"position": [0, f"{sin(1100, 90, 0.4)}", 0], "rotation": [sin(1100, 90, 3), 0, 0]},
        "tail": {"rotation": [sin(1100, 120, 6), 0, 0]},
        "head": {"rotation": [sin(1100, 200, -3), sin(90, 0, 6), 0]},
        "leg_r": {"rotation": [40, 0, 0]},
        "leg_l": {"rotation": [40, 0, 0]},
    }
    return {
        "animation.bird.fly": {"loop": True, "animation_length": 2.0, "bones": flap},
    }
