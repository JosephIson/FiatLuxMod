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
    "fallen": ("entity", "fallen", 120, 40),    # Tier 2 - Isaiah 14:12, Luke 10:18, Rev 12:9
    "mortal": ("entity", "mortal", 40, 20),               # Tier 1 - Genesis 1:27, 2:7
    "dominion": ("entity", "dominion", 250, 70),          # Tier 3 - Col 1:16
    "virtue": ("entity", "virtue", 250, 70),              # Tier 3 - Eph 1:21
    "power": ("entity", "power", 250, 70),                # Tier 3 - Eph 6:12, Jude 1:6, Rev 20:1
    "principality": ("entity", "principality", 120, 40),  # Tier 2 - Col 1:16, Dan 10:13
    "archangel": ("entity", "archangel", 120, 40),        # Tier 2 - 1 Thess 4:16, Jude 1:9
    "serpent": ("entity", "serpent", 120, 30),            # Tier 2 - Genesis 3:1, Rev 12:9
    "cross": ("entity", "cross", 20, 1),                  # prop - John 19:17-19, built to the crucified pose
    "stocks": ("entity", "stocks", 15, 1),                # prop - Jer 20:2, built to the stocks pose
    "bird": ("entity", "bird", 20, 8),                    # Tier 1 - Genesis 8:7-12, the raven and the dove
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
        if hasattr(mod, "variants"):   # same geometry, other palettes: extra textures sharing the UV layout
            import tempfile
            for vname, vmodel in mod.variants().items():
                with tempfile.TemporaryDirectory() as tmp:
                    vmodel.write(Path(tmp) / "variant.geo.json", ASSETS / f"textures/{kind}/{name}_{vname}.png")
        if args.preview:
            model.preview(args.preview / f"{name}.png")
            if hasattr(mod, "preview_pose"):
                model.preview(args.preview / f"{name}_posed.png", pose=mod.preview_pose())
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
