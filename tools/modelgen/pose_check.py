"""Poses a model and reports where its joints land in GeckoLib's in-game space (model units), so
props such as the cross and the stocks can be built to fit the poses exactly.

    python3 tools/modelgen/pose_check.py
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / "models"))
from geo import _mirror, _rot  # noqa: E402


def world(model, bone, point, pose):
    """A point given in file space, carried by `bone` through its chain under `pose`."""
    p = _mirror(point)
    for b in model._bone_chain(bone):
        r = tuple(b.rotation[i] + pose.get(b.name, (0, 0, 0))[i] for i in range(3))
        if any(r):
            p = _rot(p, _mirror(b.pivot), r)
    return p


def fmt(p):
    return "(" + ", ".join(f"{v:6.2f}" for v in p) + ")"


if __name__ == "__main__":
    import mortal
    m = mortal.build()
    pose = mortal.pose("crucified")
    for name, bone, pt in (("shoulder L", "arm_l", (4.2, 22.6, 0)), ("shoulder R", "arm_r", (-4.2, 22.6, 0)),
                           ("wrist L", "forearm_l", (4.65, 14.5, 0)), ("wrist R", "forearm_r", (-4.65, 14.5, 0)),
                           ("fingertip L", "hand_l_finger_1_tip", (4.65, 14.5 - 2.6 - 2.9, -0.32)),
                           ("fingertip R", "hand_r_finger_1_tip", (-4.65, 14.5 - 2.6 - 2.9, -0.32)),
                           ("heel L", "foot_l", (1.6, 0.0, 0.5)), ("toe L", "foot_l", (1.6, 0.0, -2.3)),
                           ("heel R", "foot_r", (-1.6, 0.0, 0.5)), ("toe R", "foot_r", (-1.6, 0.0, -2.3)),
                           ("back of torso", "torso", (0, 20.0, 1.9)), ("crown of head", "head", (0, 31.4, 0))):
        print(f"{name:14s} {fmt(world(m, bone, pt, pose))}")
