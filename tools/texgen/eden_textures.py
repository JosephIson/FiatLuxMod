"""Textures for the garden of Eden: the leaves of the tree of life and of the tree of knowledge,
their fruit hanging on the boughs, and the fruit as items.

    python3 tools/texgen/eden_textures.py
"""
import math
import random

from PIL import Image

from build_textures import clamp, save


def leaves(base, light, dark, sparkle, seed):
    """A leaf canopy tile: clustered leaves with gaps (cutout), lit on top, and sparks of colour."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if r.random() < 0.13:
                continue   # a gap between the leaves
            k = r.random()
            c = light if k > 0.72 else dark if k < 0.25 else base
            img.putpixel((x, y), (*c, 255))
    for _ in range(9):
        x, y = r.randrange(16), r.randrange(16)
        img.putpixel((x, y), (*sparkle, 255))
    return img


def fruit(skin, shine, shade, stem, leaf):
    """Fruit for the hanging-fruit model: a shaded 6x6 face at (5,5), a calyx row, a stem and a leaf."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(5, 11):
        for x in range(5, 11):
            d = math.hypot(x - 7.0, y - 7.0)
            t = min(1, d / 5.0)
            c = tuple(clamp(shine[i] * (1 - t) + skin[i] * t) for i in range(3)) if d < 2 else \
                tuple(clamp(skin[i] * (1 - (t - 0.4) * 0.6) + shade[i] * (t - 0.4) * 0.6) for i in range(3))
            img.putpixel((x, y), (*c, 255))
    for x in range(6, 10):
        img.putpixel((x, 4), (*stem, 255))
    for y in range(0, 4):
        img.putpixel((7, y), (*stem, 255))
    for x in range(12, 16):
        for y in range(0, 2):
            img.putpixel((x, y), (*leaf, 255))
    return img


def fruit_icon(skin, shine, shade, leaf, glow=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 9.0)
            if d < 5.6:
                t = d / 5.6
                hl = math.hypot(x - 5.5, y - 7.0)
                c = shine if hl < 1.6 else tuple(clamp(skin[i] * (1 - t * 0.5) + shade[i] * t * 0.5) for i in range(3))
                img.putpixel((x, y), (*c, 255))
            elif glow and d < 7.2:
                img.putpixel((x, y), (*shine, clamp(120 * (7.2 - d) / 1.6)))
    for y in range(1, 4):
        img.putpixel((8, y), (90, 60, 30, 255))
    for (x, y) in ((9, 2), (10, 2), (11, 1), (10, 1), (12, 1)):
        img.putpixel((x, y), (*leaf, 255))
    return img


def main():
    save(leaves((214, 186, 70), (255, 236, 140), (160, 130, 40), (255, 255, 220), 3), "block/life_leaves.png")
    save(leaves((40, 86, 52), (66, 120, 70), (24, 54, 34), (150, 40, 40), 5), "block/knowledge_leaves.png")
    save(fruit((255, 214, 90), (255, 255, 230), (230, 160, 40), (120, 90, 40), (200, 190, 80)), "block/life_fruit.png")
    save(fruit((190, 40, 36), (255, 200, 120), (110, 20, 24), (90, 60, 30), (60, 110, 50)), "block/knowledge_fruit.png")
    save(fruit_icon((255, 214, 90), (255, 255, 230), (230, 160, 40), (200, 190, 80), glow=True), "item/fruit_of_life.png")
    save(fruit_icon((190, 40, 36), (255, 200, 120), (110, 20, 24), (60, 110, 50)), "item/fruit_of_knowledge.png")
    print("eden textures written")


if __name__ == "__main__":
    main()
