"""Textures for the souls of the dead, made from the mortal textures so each soul keeps the face it
had in life (run after tools/modelgen/build_models.py mortal).

    python3 tools/texgen/soul_textures.py

Heaven: the same figure washed in light, glowing throughout. The Underworld: ash and shadow, with a
few embers still smouldering in it.
"""
import random
from pathlib import Path

from PIL import Image

ENTITY = Path(__file__).resolve().parents[2] / "src/main/resources/assets/fiatlux/textures/entity"
VARIANTS = ["", "_b", "_c", "_d", "_e", "_f", "_g", "_h"]


def clamp(v):
    return max(0, min(255, int(v)))


def main():
    for i, v in enumerate(VARIANTS):
        src = Image.open(ENTITY / f"mortal{v}.png").convert("RGBA")
        w, h = src.size
        heaven = Image.new("RGBA", (w, h))
        hglow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        under = Image.new("RGBA", (w, h))
        uglow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        rng = random.Random(i)
        sp, hp, hg, up, ug = src.load(), heaven.load(), hglow.load(), under.load(), uglow.load()
        for y in range(h):
            for x in range(w):
                r, g, b, a = sp[x, y]
                if a == 0:
                    hp[x, y] = up[x, y] = (0, 0, 0, 0)
                    continue
                lum = 0.3 * r + 0.59 * g + 0.11 * b
                # light: toward warm white, keeping a trace of the living colours
                k = 0.76
                c = (r + (255 - r) * k, g + (248 - g) * k, b + (226 - b) * k)
                hp[x, y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), a)
                hg[x, y] = (clamp(c[0] * 0.9), clamp(c[1] * 0.88), clamp(c[2] * 0.8), a)
                # ash: grey shadow with a faint red-brown cast, and a few smouldering embers
                ash = lum * 0.5 + 18
                up[x, y] = (clamp(ash * 1.1), clamp(ash * 0.95), clamp(ash * 0.92), a)
                if rng.random() < 0.025:
                    e = (255, 90 + rng.randint(0, 60), 20)
                    up[x, y] = (*e, a)
                    ug[x, y] = (*e, 255)
        n = "" if i == 0 else v
        heaven.save(ENTITY / f"soul_heaven{n}.png")
        hglow.save(ENTITY / f"soul_heaven{n}_glowmask.png")
        under.save(ENTITY / f"soul_underworld{n}.png")
        uglow.save(ENTITY / f"soul_underworld{n}_glowmask.png")
    print("soul textures written")


if __name__ == "__main__":
    main()
