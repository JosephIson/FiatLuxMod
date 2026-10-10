import sys
sys.path[:0] = ['.', 'models']
import seraph
from coverage import visible_fraction
m = seraph.build()
face = {n for n in m.bones if n == "head" or n.startswith("head_")}
feet = {"leg_l", "leg_r"}
for name, group in (("face", face), ("feet+legs", feet)):
    frac, n = visible_fraction(m, group, step=0.35)
    print(f"{name}: {frac * 100:.1f}% visible from the front ({n} rays)")
