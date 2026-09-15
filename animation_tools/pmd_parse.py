#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PMD (Polygon Model Data) parser - extract bone hierarchy."""
import struct
import sys


def read_cstr(buf, offset, length, encoding="shift_jis"):
    raw = buf[offset:offset + length]
    end = raw.find(b"\x00")
    if end != -1:
        raw = raw[:end]
    try:
        return raw.decode(encoding)
    except Exception:
        return raw.decode(encoding, errors="replace")


def parse_pmd(path):
    with open(path, "rb") as f:
        buf = f.read()
    off = 0
    magic = buf[off:off + 3]
    off += 3
    version = struct.unpack_from("<f", buf, off)[0]
    off += 4
    model_name = read_cstr(buf, off, 20)
    off += 20
    comment = read_cstr(buf, off, 256)
    off += 256

    # vertices (38 bytes each)
    vcount = struct.unpack_from("<I", buf, off)[0]
    off += 4 + vcount * 38

    # indices
    icount = struct.unpack_from("<I", buf, off)[0]
    off += 4 + icount * 2

    # materials (70 bytes each)
    mcount = struct.unpack_from("<I", buf, off)[0]
    off += 4
    mats = []
    for _ in range(mcount):
        mats.append(read_cstr(buf, off + 50, 20))
        off += 70

    # bones (count is signed short; 39 bytes each)
    bcount = struct.unpack_from("<h", buf, off)[0]
    off += 2
    bones = []
    for i in range(bcount):
        name = read_cstr(buf, off, 20)
        parent = struct.unpack_from("<h", buf, off + 20)[0]
        child = struct.unpack_from("<h", buf, off + 22)[0]
        btype = buf[off + 24]
        ik = struct.unpack_from("<h", buf, off + 25)[0]
        pos = struct.unpack_from("<3f", buf, off + 26)
        off += 39
        bones.append({"index": i, "name": name, "parent": parent, "child": child,
                      "type": btype, "ik": ik, "pos": [round(v, 4) for v in pos]})

    return {"magic": magic, "version": version, "model_name": model_name,
            "comment": comment, "bone_count": bcount, "bones": bones,
            "material_count": mcount, "materials": mats[:10],
            "file_size": len(buf)}


def main(path):
    d = parse_pmd(path)
    print(f"== {path} ==")
    print(f"magic={d['magic']} version={d['version']} model={d['model_name']!r}")
    print(f"comment={d['comment']!r}")
    print(f"bones={d['bone_count']} materials={d['material_count']} size={d['file_size']}")
    print("\n--- bones (hierarchy) ---")
    for b in d["bones"]:
        pname = d["bones"][b["parent"]]["name"] if 0 <= b["parent"] < len(d["bones"]) else "ROOT"
        cname = d["bones"][b["child"]]["name"] if 0 <= b["child"] < len(d["bones"]) else "-"
        print(f"  [{b['index']:2d}] {b['name']:<16} parent={b['parent']:2d}({pname:<12}) "
              f"child={b['child']:2d}({cname:<12}) type={b['type']} pos=({b['pos'][0]:9.4f},{b['pos'][1]:9.4f},{b['pos'][2]:9.4f})")


if __name__ == "__main__":
    main(sys.argv[1])
