"""Regenerates every procedural GeckoLib asset into src/main/resources/assets/fiatlux.

    python3 tools/modelgen/build_models.py [--preview DIR]

Each model module exposes build() -> geo.Model and animations() -> dict.
MODELS maps module -> (asset kind, asset name, minimum cubes, minimum bones) so the
detail-tier standards from the design doc are enforced at generation time.
"""
import argparse
import importlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / "models"))
from geo import write_animations  # noqa: E402

ASSETS = HERE.parents[1] / "src/main/resources/assets/fiatlux"

# module: (kind, asset name, min cubes, min bones)
MODELS = {
    "flaming_sword": ("item", "flaming_sword_of_cherubim", 40, 8),
    "angel": ("entity", "angel", 120, 40),      # Tier 2 - Daniel 10:5-6
    "seraph": ("entity", "seraph", 250, 70),    # Tier 3 - Isaiah 6:2
    "cherub": ("entity", "cherub", 250, 70),    # Tier 3 - Ezekiel 1:5-13, 10:12-14
    "ophan": ("entity", "ophan", 250, 70),      # Tier 3 - Ezekiel 1:15-21, 10:9-13
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", type=Path)
    ap.add_argument("only", nargs="*")
    args = ap.parse_args()
    failed = False
    for module, (kind, name, min_cubes, min_bones) in MODELS.items():
        if args.only and module not in args.only:
            continue
        mod = importlib.import_module(module)
        model = mod.build()
        stats = model.write(ASSETS / f"geo/{kind}/{name}.geo.json", ASSETS / f"textures/{kind}/{name}.png")
        write_animations(ASSETS / f"animations/{kind}/{name}.animation.json", mod.animations())
        ok = stats["cubes"] >= min_cubes and stats["bones"] >= min_bones
        failed |= not ok
        print(f"{'OK ' if ok else 'LOW'} {kind}/{name}: {stats['cubes']} cubes, {stats['bones']} bones, "
              f"{model.tex_w}x{model.tex_h} atlas (tier minimum {min_cubes}/{min_bones})")
        if args.preview:
            model.preview(args.preview / f"{name}.png")
            if hasattr(mod, "preview_pose"):
                model.preview(args.preview / f"{name}_posed.png", pose=mod.preview_pose())
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
