#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""VMD (Vocaloid Motion Data 0002) binary writer."""
import struct


def encode_sjis(name, length):
    b = name.encode("shift_jis", errors="replace")
    if len(b) > length:
        b = b[:length]
    return b + b"\x00" * (length - len(b))


# Default interpolation block: linear (values are ignored by NyMmd anyway, but
# keep the MMD convention: all 0x14 = "smooth" control points).
_LINEAR_INTERP = bytes([0x14] * 64)


def write_bone_frame(buf, name, frame, pos, rot):
    buf += encode_sjis(name, 15)
    buf += struct.pack("<I", int(frame))
    buf += struct.pack("<3f", *pos)
    buf += struct.pack("<4f", *rot)
    buf += _LINEAR_INTERP


def write_vmd(path, model_name, bone_tracks):
    """bone_tracks: list of (bone_name, [(frame, pos3, rot4), ...]) sorted by frame."""
    buf = bytearray()
    header = b"Vocaloid Motion Data 0002"
    buf += header + b"\x00" * (30 - len(header))
    buf += encode_sjis(model_name, 20)
    # dedupe/merge same-frame keys per bone
    merged = []
    for name, keys in bone_tracks:
        by_frame = {}
        for f, pr in keys:
            by_frame[int(f)] = pr
        frames = sorted(by_frame.keys())
        merged.append((name, [(f, by_frame[f][0], by_frame[f][1]) for f in frames]))
    total_keys = sum(len(k) for _, k in merged)
    buf += struct.pack("<I", total_keys)
    for name, keys in merged:
        for f, p, r in keys:
            write_bone_frame(buf, name, f, p, r)
    # no morph / camera / light / shadow
    buf += struct.pack("<I", 0)
    buf += struct.pack("<I", 0)
    buf += struct.pack("<I", 0)
    buf += struct.pack("<I", 0)
    with open(path, "wb") as f:
        f.write(buf)
    return len(buf)
