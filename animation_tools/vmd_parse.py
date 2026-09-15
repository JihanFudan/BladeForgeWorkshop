#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""VMD (Vocaloid Motion Data 0002) parser + dumper.
Usage:
    python vmd_parse.py <file.vmd> [--json out.json] [--csv out.csv]
"""
import json
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


def parse_vmd(path):
    with open(path, "rb") as f:
        buf = f.read()
    off = 0
    header = buf[off:off + 30].decode("ascii", errors="replace")
    off += 30
    model_name = read_cstr(buf, off, 20)
    off += 20

    # --- bone frames ---
    bone_count = struct.unpack_from("<I", buf, off)[0]
    off += 4
    bones = []
    for _ in range(bone_count):
        name = read_cstr(buf, off, 15)
        off += 15
        frame = struct.unpack_from("<I", buf, off)[0]
        off += 4
        pos = struct.unpack_from("<3f", buf, off)
        off += 12
        rot = struct.unpack_from("<4f", buf, off)
        off += 16
        interp = buf[off:off + 64]
        off += 64
        bones.append({
            "name": name, "frame": frame,
            "pos": [round(v, 6) for v in pos],
            "rot": [round(v, 6) for v in rot],
        })

    # --- morph frames ---
    morph_count = struct.unpack_from("<I", buf, off)[0] if off + 4 <= len(buf) else 0
    off += 4
    morphs = []
    for _ in range(morph_count):
        name = read_cstr(buf, off, 15)
        off += 15
        frame = struct.unpack_from("<I", buf, off)[0]
        off += 4
        weight = struct.unpack_from("<f", buf, off)[0]
        off += 4
        morphs.append({"name": name, "frame": frame, "weight": round(weight, 6)})

    # --- camera frames (skip payload) ---
    cam_count = struct.unpack_from("<I", buf, off)[0] if off + 4 <= len(buf) else 0
    off += 4
    off += cam_count * 61

    # --- light frames ---
    light_count = struct.unpack_from("<I", buf, off)[0] if off + 4 <= len(buf) else 0
    off += 4
    off += light_count * 28

    # --- self shadow frames ---
    shadow_count = struct.unpack_from("<I", buf, off)[0] if off + 4 <= len(buf) else 0
    off += 4
    off += shadow_count * 17

    return {
        "header": header,
        "model_name": model_name,
        "bone_count": bone_count,
        "bones": bones,
        "morph_count": morph_count,
        "morphs": morphs,
        "camera_count": cam_count,
        "light_count": light_count,
        "shadow_count": shadow_count,
        "file_size": len(buf),
    }


def dump_text(path):
    data = parse_vmd(path)
    print(f"== {path} ==")
    print(f"header={data['header']!r} model={data['model_name']!r} "
          f"bones={data['bone_count']} morphs={data['morph_count']} "
          f"cams={data['camera_count']} lights={data['light_count']} shadows={data['shadow_count']} "
          f"size={data['file_size']}")
    by_bone = {}
    for b in data["bones"]:
        by_bone.setdefault(b["name"], []).append(b)
    for name, frames in by_bone.items():
        frames_sorted = sorted(frames, key=lambda f: f["frame"])
        print(f"\n--- bone '{name}' ({len(frames_sorted)} keys) ---")
        for f in frames_sorted:
            print(f"  f{f['frame']:>4} pos=({f['pos'][0]:9.4f},{f['pos'][1]:9.4f},{f['pos'][2]:9.4f}) "
                  f"rot=({f['rot'][0]:8.5f},{f['rot'][1]:8.5f},{f['rot'][2]:8.5f},{f['rot'][3]:8.5f})")


if __name__ == "__main__":
    dump_text(sys.argv[1])
