#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Preview VMD inspect animations with the corrected 1.21.1 render chain.

Chain (verified): eye = (0.15*px, 0.125*py - 1.5, -0.125*pz - 0.5),
depth = 0.125*pz + 0.5.  Tip dir in eye = (1.2*R00, R10, -R20).
Blade model: tip at OBJ -X (297.12 units), back at OBJ +X (70.71 units);
world scale = 0.0625 * 0.125 = 0.0078125.

Usage: python preview.py <vmd> <frame_list> <out_png> [cols]
   frame_list: comma-separated frames, e.g. 0,19,40,60,84
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vmd_gen import quat_to_mat

FOV_V = math.radians(70.0)
FY = (1.0 / math.tan(FOV_V / 2.0))
FX = FY

BLADE_LEN = 297.12 * 0.0078125      # 2.321 (tip extent)
BACK_LEN = 70.71 * 0.0078125        # 0.552 (grip/sheath back extent)


def read_keys(path):
    """Return dict bone -> [(frame, pos3, rot4)]."""
    bones = {}
    data = open(path, "rb").read()
    off = 30 + 20
    n = int.from_bytes(data[off:off + 4], "little")
    off += 4
    for _ in range(n):
        name = data[off:off + 15].split(b"\x00")[0].decode("shift_jis", "replace")
        off += 15
        fr = int.from_bytes(data[off:off + 4], "little")
        off += 4
        pos = np.array(struct_unpack("<3f", data, off)); off += 12
        rot = np.array(struct_unpack("<4f", data, off)); off += 16
        off += 64  # interpolation
        bones.setdefault(name, []).append((fr, pos, rot))
    return bones


def struct_unpack(fmt, data, off):
    import struct
    return struct.unpack_from(fmt, data, off)


def eye_of(pos):
    px, py, pz = pos
    return np.array([0.15 * px, 0.125 * py - 1.5, -0.125 * pz - 0.5])


def tip_dir_eye(rot):
    R = quat_to_mat(rot)
    return np.array([1.2 * R[0, 0], R[1, 0], -R[2, 0]])


def front_dir_eye(rot):
    R = quat_to_mat(rot)
    return np.array([-1.2 * R[0, 2], -R[1, 2], R[2, 2]])


def project(e):
    """eye pos -> (sx, sy) pixels from the center at unit scale."""
    d = -e[2]                      # depth: positive forward
    if d < 0.05:
        return None
    return (e[0] / d * FX, -e[1] / d * FY)


def draw_blade(draw, center, rot, scale, color, width=3):
    """Draw the blade segment (back..hardpoint..tip) + tip tick."""
    e = eye_of(center)
    d0 = project(e)
    if d0 is None:
        return
    tdir = tip_dir_eye(rot)
    tip = e + BLADE_LEN * tdir
    back = e - BACK_LEN * tdir
    p_tip = project(tip)
    p_back = project(back)
    sx0, sy0 = d0
    if p_tip is not None:
        draw.line([sx0 * scale + 640, sy0 * scale + 360,
                   p_tip[0] * scale + 640, p_tip[1] * scale + 360],
                  fill=color, width=width)
    if p_back is not None:
        draw.line([sx0 * scale + 640, sy0 * scale + 360,
                   p_back[0] * scale + 640, p_back[1] * scale + 360],
                  fill=color, width=width)
    # hardpoint dot
    r = 4
    draw.ellipse([sx0 * scale + 640 - r, sy0 * scale + 360 - r,
                  sx0 * scale + 640 + r, sy0 * scale + 360 + r], fill=color)


def draw_front(draw, center, rot, scale, color=(0, 200, 80), width=2):
    e = eye_of(center)
    d0 = project(e)
    if d0 is None:
        return
    fdir = front_dir_eye(rot)
    p2 = project(e + 0.25 * fdir)
    if p2 is None:
        return
    draw.line([d0[0] * scale + 640, d0[1] * scale + 360,
               p2[0] * scale + 640, p2[1] * scale + 360], fill=color, width=width)


def render_frame(bones, fr, size=(1280, 720)):
    W, H = size
    img = Image.new("RGB", size, (90, 105, 130))
    d = ImageDraw.Draw(img)
    foc = (H / 2.0) / math.tan(FOV_V / 2.0)
    cx, cy = W / 2.0, H / 2.0
    d.line([cx, 0, cx, H], fill=(70, 80, 100), width=1)
    d.line([0, cy, W, cy], fill=(70, 80, 100), width=1)

    def proj(e):
        dep = -e[2]
        if dep < 0.05:
            return None
        return (cx + e[0] / dep * foc, cy - e[1] / dep * foc)

    def draw_blade(center, rot, color, width):
        e = eye_of(center)
        p0 = proj(e)
        if p0 is None:
            return
        tdir = tip_dir_eye(rot)
        p_tip = proj(e + BLADE_LEN * tdir)
        p_back = proj(e - BACK_LEN * tdir)
        if p_tip is not None:
            d.line([p0, p_tip], fill=color, width=width)
        if p_back is not None:
            d.line([p0, p_back], fill=color, width=width)
        r = 3
        d.ellipse([p0[0] - r, p0[1] - r, p0[0] + r, p0[1] + r], fill=color)

    for name, color, w in (("hardpointB", (30, 30, 40), 5), ("hardpointA", (235, 235, 240), 3)):
        keys = bones.get(name, [])
        pos = rot = None
        for f, p, r in keys:
            if f <= fr:
                pos, rot = p, r
            else:
                break
        if pos is None:
            continue
        draw_blade(pos, rot, color, w)
        if name == "hardpointA":
            e = eye_of(pos)
            p0 = proj(e)
            fdir = front_dir_eye(rot)
            p2 = proj(e + 0.25 * fdir)
            if p0 is not None and p2 is not None:
                d.line([p0, p2], fill=(0, 200, 80), width=2)
    d.text((6, 6), f"frame {fr}", fill=(255, 255, 255))
    return img


def contact_sheet(vmd_path, frames, out_png, cols=6):
    bones = read_keys(vmd_path)
    cell = (320, 180)
    rows = (len(frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cell[0] * cols, cell[1] * rows), (40, 45, 60))
    for i, fr in enumerate(frames):
        img = render_frame(bones, fr, cell)
        sheet.paste(img, ((i % cols) * cell[0], (i // cols) * cell[1]))
    sheet.save(out_png)
    print(f"saved {out_png}  {sheet.size}")


def main():
    vmd = sys.argv[1]
    frames = [int(x) for x in sys.argv[2].split(",")]
    out = sys.argv[3]
    cols = int(sys.argv[4]) if len(sys.argv) > 4 else 6
    contact_sheet(vmd, frames, out, cols)


if __name__ == "__main__":
    main()
