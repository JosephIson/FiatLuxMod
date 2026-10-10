"""Writes data/fiatlux/structure/empty.nbt: a 5x5x5 air template for GameTests."""
import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/data/fiatlux/structure/empty.nbt"


def s(txt):
    b = txt.encode()
    return struct.pack(">H", len(b)) + b


def tag(t, name, payload):
    return bytes([t]) + s(name) + payload


def compound(items):
    return b"".join(items) + b"\x00"


def int_list(name, vals):
    return tag(9, name, bytes([3]) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals))


def compound_list(name, comps):
    return tag(9, name, bytes([10 if comps else 0]) + struct.pack(">i", len(comps)) + b"".join(compound(c) for c in comps))


root = compound([
    tag(3, "DataVersion", struct.pack(">i", 3955)),
    int_list("size", [5, 5, 5]),
    compound_list("palette", [[tag(8, "Name", s("minecraft:air"))]]),
    compound_list("blocks", []),
    compound_list("entities", []),
])
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(gzip.compress(bytes([10]) + s("") + root))
print("wrote", OUT)
