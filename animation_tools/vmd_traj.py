#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Analyze a VMD: print hardpointA trajectory summary + phases."""
import sys
import math
from vmd_parse import parse_vmd

d = parse_vmd(sys.argv[1])
bones = {}
for b in d["bones"]:
    bones.setdefault(b["name"], {})[b["frame"]] = (b["pos"], b["rot"])

def quat_to_euler(q):
    x, y, z, w = q
    # yaw (Z), pitch (X), roll (Y) approx for trajectory viz
    t0 = 2.0 * (w * x + y * z)
    t1 = 1.0 - 2.0 * (x * x + y * y)
    rx = math.atan2(t0, t1)
    t2 = 2.0 * (w * y - z * x)
    t2 = max(-1.0, min(1.0, t2))
    ry = math.asin(t2)
    t3 = 2.0 * (w * z + x * y)
    t4 = 1.0 - 2.0 * (y * y + z * z)
    rz = math.atan2(t3, t4)
    return math.degrees(rx), math.degrees(ry), math.degrees(rz)

for bone_name in ("hardpointA", "hardpointB"):
    if bone_name not in bones:
        continue
    frames = sorted(bones[bone_name].keys())
    print(f"== {bone_name}: {len(frames)} keys ==")
    prev = None
    for f in frames:
        pos, rot = bones[bone_name][f]
        dx = pos[0] - prev[0] if prev else 0.0
        dy = pos[1] - prev[1] if prev else 0.0
        dz = pos[2] - prev[2] if prev else 0.0
        dist = math.sqrt(dx*dx + dy*dy + dz*dz)
        e = quat_to_euler(rot)
        marker = ""
        if prev is not None and dist > 0.01:
            marker = f"  move {dist:6.2f}"
        print(f"f{f:>4} p=({pos[0]:8.2f},{pos[1]:8.2f},{pos[2]:8.2f}) "
              f"e=({e[0]:7.1f},{e[1]:7.1f},{e[2]:7.1f}){marker}")
        prev = pos
