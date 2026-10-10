"""Front-view occlusion check: what fraction of a bone group is visible to a viewer standing
in front of the model (-z)? Used to verify that the seraph's veiling wings cover the face
and feet (Isaiah 6:2). Rays are cast along +z in GeckoLib's in-game space."""


def _tri_hit(o, tri):
    """Ray o + t*(0,0,1) vs triangle; returns t or None (Möller–Trumbore, d fixed to +z)."""
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = tri
    e1 = (bx - ax, by - ay, bz - az)
    e2 = (cx - ax, cy - ay, cz - az)
    # p = d x e2 with d = (0,0,1)
    p = (-e2[1], e2[0], 0.0)
    det = e1[0] * p[0] + e1[1] * p[1] + e1[2] * p[2]
    if abs(det) < 1e-9:
        return None
    inv = 1.0 / det
    s = (o[0] - ax, o[1] - ay, o[2] - az)
    u = (s[0] * p[0] + s[1] * p[1] + s[2] * p[2]) * inv
    if u < 0 or u > 1:
        return None
    q = (s[1] * e1[2] - s[2] * e1[1], s[2] * e1[0] - s[0] * e1[2], s[0] * e1[1] - s[1] * e1[0])
    v = q[2] * inv
    if v < 0 or u + v > 1:
        return None
    return (e2[0] * q[0] + e2[1] * q[1] + e2[2] * q[2]) * inv


def visible_fraction(model, target_bones, step=0.25):
    owner = {}
    for b in model.bones.values():
        for c in b.cubes:
            owner[id(c)] = b.name
    faces = []
    bone_of = []
    for b in model.bones.values():
        for c in b.cubes:
            pass
    # world_polys does not expose the cube, so rebuild the mapping in iteration order
    order = [b.name for b in model.bones.values() for c in b.cubes for _ in range(6)]
    for (mat, f, pts, rect), bone in zip(model.world_polys(), order):
        faces.append(((pts[0], pts[1], pts[2]), (pts[0], pts[2], pts[3])))
        bone_of.append(bone)
    tgt = [i for i, b in enumerate(bone_of) if b in target_bones]
    xs = [p[0] for i in tgt for tri in faces[i] for p in tri]
    ys = [p[1] for i in tgt for tri in faces[i] for p in tri]
    total = seen = 0
    x = min(xs)
    while x <= max(xs):
        y = min(ys)
        while y <= max(ys):
            o = (x, y, -1000.0)
            best_t, best_bone, tgt_hit = None, None, False
            for i, tris in enumerate(faces):
                for tri in tris:
                    t = _tri_hit(o, tri)
                    if t is not None and (best_t is None or t < best_t):
                        best_t, best_bone = t, bone_of[i]
            # only count rays that would hit the target if nothing else were there
            if any(_tri_hit(o, tri) is not None for i in tgt for tri in faces[i]):
                total += 1
                seen += best_bone in target_bones
            y += step
        x += step
    return seen / total if total else 0.0, total
