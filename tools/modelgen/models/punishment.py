"""Instruments of human justice, built to fit the mortal's held poses exactly (see pose_check.py).

The cross: a rough-hewn upright stake set in a mound of stones, a crossbeam lashed across it, a
footrest, the board of the charge (titulus) above, and cords binding the wrists to the beam - "they
crucified him, and two other with him" (John 19:18). Shown with dignity: there is no blood.
The stocks (a standing pillory): two posts and a board with holes for the head and wrists.

Measurements come from the mortal posed and rendered at 0.95 scale, standing on the footrest (the
mortal's feet are FOOT units above the ground on the cross) and facing -z like the prop.
"""
from geo import Material, Model

TIMBER = Material("p_timber", (122, 92, 62), "wood", accent=(150, 116, 80))
TIMBER_DARK = Material("p_timber_dark", (92, 68, 46), "wood", accent=(118, 88, 60))
ROPE = Material("p_rope", (176, 150, 104), "cloth", accent=(150, 126, 84))
STONE = Material("p_stone", (128, 124, 116), "plain")
EARTH = Material("p_earth", (110, 84, 60), "plain")
BOARD = Material("p_board", (206, 190, 156), "wood", accent=(180, 160, 120))
IRON = Material("p_iron", (90, 90, 96), "metal")

S = 0.95             # the mortal's render scale
FR = 14.0            # top of the footrest
FOOT = FR + 0.9      # the mortal's origin on the cross (toes rest on the footrest)


def scratches(px, gp, rect):
    """The charge, scratched on the titulus in three lines of unreadable letters."""
    rx, ry, rw, rh = rect
    for row in range(3):
        y = ry + 2 + row * max(2, (rh - 4) // 3)
        x = rx + 2
        while x < rx + rw - 3:
            wlen = 2 + (x * 7 + row * 3) % 4
            for i in range(wlen):
                if x + i < rx + rw - 2 and y < ry + rh - 1:
                    px[x + i, y] = (60, 44, 30, 255)
            x += wlen + 1
    return False


def build_cross():
    m = Model("geometry.fiatlux.cross", visible_bounds=(3, 4, (0, 1.6, 0)), density=3)
    m.bone("root", (0, 0, 0))
    # the mound of earth and stones the stake is set in
    m.cube("root", (-6, 0, -1), (12, 2, 14), EARTH)
    for x, z, sx, sz, h in ((-7, 0, 3, 3, 2.5), (4, 1, 3.5, 3, 3), (-5, 9, 4, 3, 2.2), (3, 9, 3, 4, 2.8), (-1, 12, 3, 2, 2)):
        m.cube("root", (x, 0, z), (sx, h, sz), STONE)
    # the upright stake (stipes): rough-hewn, a little uneven
    m.cube("root", (-3, 0, 2.1), (6, 58, 6), TIMBER)
    m.cube("root", (-3.3, 4, 2.0), (6.6, 10, 6.2), TIMBER_DARK)          # weathered lower trunk
    m.cube("root", (-2.6, 52, 2.4), (5.2, 6.5, 5.4), TIMBER)              # tapering top
    # the crossbeam (patibulum), just in front of the stake, where the arms lie
    beam_y = FOOT + 23.0 * S
    m.cube("root", (-20, beam_y - 2, 1.4), (40, 4, 3.6), TIMBER)
    m.cube("root", (-20.3, beam_y - 1.6, 1.3), (0.6, 3.2, 3.8), TIMBER_DARK)
    m.cube("root", (19.7, beam_y - 1.6, 1.3), (0.6, 3.2, 3.8), TIMBER_DARK)
    # the lashing where beam meets stake
    for dx in (-3.4, 2.4):
        m.cube("root", (dx, beam_y - 2.4, 1.1), (1.0, 4.8, 4.4), ROPE, inflate=0.05)
    m.cube("root", (-3.6, beam_y - 0.4, 1.0), (7.2, 0.8, 4.6), ROPE, inflate=0.05)
    # cords binding the wrists to the beam
    for side in (1, -1):
        x = side * 12.3 * S
        m.cube("root", (x - 0.7, beam_y - 2.3, 0.6), (1.4, 4.6, 4.6), ROPE, inflate=0.05)
        m.cube("root", (x - 0.9, beam_y - 2.0, 0.4), (1.8, 0.6, 1.0), ROPE)
    # the footrest (suppedaneum) the feet rest on, and a cord about the ankles
    m.cube("root", (-3.5, FR - 1.6, -2.6), (7, 1.6, 4.8), TIMBER_DARK)
    m.cube("root", (-3.2, FR - 4.5, 1.0), (6.4, 3.0, 1.2), TIMBER_DARK)   # its brace
    m.cube("root", (-2.6, FOOT + 1.2, -1.2), (5.2, 0.9, 3.4), ROPE, inflate=0.05)
    # the board of the charge (titulus), above the head
    m.cube("root", (-7.5, 46.5, 1.1), (15, 6.5, 1.0), BOARD)            # the charge is written on it in game
    for x in (-7.0, 6.2):
        m.cube("root", (x, 51.9, 0.9), (0.8, 0.8, 0.3), IRON)             # the nails that hold it, at its corners
    return m


def build_stocks():
    m = Model("geometry.fiatlux.stocks", visible_bounds=(3, 3, (0, 1, 0)), density=3)
    m.bone("root", (0, 0, 0))
    bz = -6.9 * S            # the plane of the board: where the neck and wrists are held
    neck_y, wrist_y, wrist_x = 20.4 * S, 20.6 * S, 5.96 * S
    z0, depth = bz - 1.1, 2.2
    # two posts and a cap
    for x in (-12.5, 10.5):
        m.cube("root", (x, 0, z0 - 0.2), (2, 25.5, 2.6), TIMBER)
        m.cube("root", (x - 1, 0, z0 - 1.2), (4, 1.5, 4.6), TIMBER_DARK)    # feet of the posts
    m.cube("root", (-13.5, 25.5, z0 - 0.5), (27, 1.6, 3.2), TIMBER_DARK)
    # the board, built around three holes: the neck between the two wrists
    holes = [(-1.7, 1.7, neck_y - 1.6, neck_y + 1.8), (wrist_x - 0.85, wrist_x + 0.85, wrist_y - 0.85, wrist_y + 0.85),
             (-wrist_x - 0.85, -wrist_x + 0.85, wrist_y - 0.85, wrist_y + 0.85)]
    y_lo, y_hi = neck_y - 3.6, neck_y + 3.4
    m.cube("root", (-10.5, y_lo, z0), (21, (neck_y - 1.6) - y_lo - 0.01 if False else 2.0, depth), BOARD)
    # strips between and around the holes, so the holes stay open
    xs = sorted([-10.5, 10.5] + [h[0] for h in holes] + [h[1] for h in holes])
    for i in range(len(xs) - 1):
        a, b = xs[i], xs[i + 1]
        inside = [h for h in holes if h[0] <= a and b <= h[1]]
        if inside:
            h = inside[0]
            if h[2] - y_lo > 2.0:
                m.cube("root", (a, y_lo + 2.0, z0), (b - a, h[2] - y_lo - 2.0, depth), BOARD)
            m.cube("root", (a, h[3], z0), (b - a, y_hi - h[3], depth), BOARD)
        else:
            m.cube("root", (a, y_lo + 2.0, z0), (b - a, y_hi - y_lo - 2.0, depth), BOARD)
    m.cube("root", (-10.5, y_hi, z0 - 0.1), (21, 0.6, depth + 0.2), TIMBER_DARK)   # the hinge rail
    for x in (-9.5, 8.5):
        m.cube("root", (x, neck_y - 0.3, z0 - 0.3), (1.0, 0.6, depth + 0.6), IRON)    # the hasp
    # a placard on the cap naming the offender and the offence (written on it in game)
    m.cube("root", (-7.5, 27.1, z0 - 0.2), (15, 5.0, 0.8), BOARD)
    for x in (-7.0, 6.2):
        m.cube("root", (x, 31.3, z0 - 0.35), (0.8, 0.6, 0.3), IRON)
    return m


def build():
    return build_cross()


def animations():
    return {}
