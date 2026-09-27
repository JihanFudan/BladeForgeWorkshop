# -*- coding: utf-8 -*-
"""
preview_assembly.py —— 纯 Python(PIL) 软件渲染组装动画预览，用于"渲图自查"。

读取游戏同源数据（blade_parts.json 网格 + assembly_vmd.py 编排），
按正交前视把指定帧画成网格图，检查：部件是否横放、落座是否对齐、流程是否连贯。

用法: python3 animation_tools/preview_assembly.py [输出png]
默认输出 model-previews/assembly_grid.png
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

import assembly_vmd  # noqa: E402

JSON_PATH = os.path.join(ROOT, "src", "main", "resources", "assets",
                         "slashbladeresh_slashblad", "assembly", "blade_parts.json")
TEX_PATH = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "slashbladeresh_slashblad", "textures", "assembly", "sb_wood.png")
DEFAULT_OUT = os.path.join(ROOT, "model-previews", "assembly_grid.png")

FRAMES = [0, 8, 12, 20, 30, 42, 52, 66, 75, 86, 99, 110]
CELL_W, CELL_H = 480, 270
ZOOM = 210.0          # 每格像素
PART_ORDER = ["blade_blank", "tsuba", "handle", "sheath"]


def load():
    doc = json.load(open(JSON_PATH, encoding="utf-8"))
    parts = {}
    for name, arr in doc["parts"].items():
        tris = []
        for k in range(0, len(arr), 15):
            v = arr[k:k + 15]
            tris.append(((v[0], v[1], v[2]), (v[3], v[4]),
                         (v[5], v[6], v[7]), (v[8], v[9]),
                         (v[10], v[11], v[12]), (v[13], v[14])))
        parts[name] = tris
    tex = Image.open(TEX_PATH).convert("RGBA")
    return parts, tex


def avg_color(tex, uv0, uv1, uv2):
    u = (uv0[0] + uv1[0] + uv2[0]) / 3.0
    v = (uv0[1] + uv1[1] + uv2[1]) / 3.0
    x = min(tex.width - 1, max(0, int(u * tex.width)))
    y = min(tex.height - 1, max(0, int(v * tex.height)))
    r, g, b, a = tex.getpixel((x, y))
    if a < 128:
        return (120, 120, 130)
    return (r, g, b)


def render_frame(parts, tex, frame, tracks):
    img = Image.new("RGB", (CELL_W, CELL_H), (16, 16, 22))
    d = ImageDraw.Draw(img)
    # 地平线参考
    d.line([(0, CELL_H * 0.62), (CELL_W, CELL_H * 0.62)], fill=(30, 30, 40))
    tris = []
    for name in PART_ORDER:
        dense = tracks[name]
        x, y, z = dense[min(frame, len(dense) - 1)][1]
        roll = math.radians(dense[min(frame, len(dense) - 1)][2])
        cr, sr = math.cos(roll), math.sin(roll)
        for (p0, u0, p1, u1, p2, u2) in parts[name]:
            pts = []
            zs = 0.0
            for (px, py, pz) in (p0, p1, p2):
                rx = px * cr - py * sr + x
                ry = px * sr + py * cr + y
                rz = pz + z
                zs += rz
                pts.append((CELL_W / 2 + rx * ZOOM, CELL_H / 2 - ry * ZOOM))
            col = avg_color(tex, u0, u1, u2)
            # 法线 z 分量做简单明暗
            ax, ay = pts[0]; bx, by = pts[1]; cx2, cy2 = pts[2]
            area = (bx - ax) * (cy2 - ay) - (by - ay) * (cx2 - ax)
            shade = 0.62 + 0.38 * min(1.0, abs(area) / 800.0)
            if area < 0:
                shade *= 0.86
            col = tuple(int(c * shade) for c in col)
            tris.append((zs / 3.0, pts, col))
    tris.sort(key=lambda t: -t[0])  # 远在下先画（z 朝屏幕外越大越近）
    for _, pts, col in tris:
        d.polygon(pts, fill=col, outline=(max(0, col[0] - 40), max(0, col[1] - 40), max(0, col[2] - 40)))
    d.text((8, 6), f"f{frame:03d}", fill=(200, 200, 90))
    return img


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    parts, tex = load()
    tracks = {name: dense for name, dense in assembly_vmd.build_stage_tracks()}
    cols = 4
    rows = (len(FRAMES) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * CELL_W, rows * (CELL_H + 22)), (8, 8, 12))
    for i, f in enumerate(FRAMES):
        cell = render_frame(parts, tex, f, tracks)
        sheet.paste(cell, ((i % cols) * CELL_W, (i // cols) * (CELL_H + 22)))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
    print("OK", out, sheet.size)


if __name__ == "__main__":
    main()
