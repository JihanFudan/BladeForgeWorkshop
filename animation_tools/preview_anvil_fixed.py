# -*- coding: utf-8 -*-
"""
preview_anvil_fixed.py —— 自查"烧红锭子平放在锻造铁砧台面"的最终观感。

复刻 Java 侧 FloatingItem + FIXED display 的完整变换链：
  v_final = T · Rx(90°) · S(台面缩放) · FIXED display(rx0,ry180,rz57,s1.5) · (mesh)
再用 preview_held_models 的渲染器从斜上方相机画出来，对比：
  旧：hold_ingot（侧立）  新：hold_ingot_flat（宽面朝上）× 三种材质表。

用法: python3 animation_tools/preview_anvil_fixed.py
输出: model-previews/anvil_fixed_check.png
"""
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

from preview_held_models import apply_display, load_parts, render, SHEET_DIR  # noqa: E402

NS = "slashbladeresh_slashblad"
OUT = os.path.join(ROOT, "model-previews", "anvil_fixed_check.png")


def to_block(tris, scale):
    """FIXED display → FloatingItem 的 Rx90 与台面缩放，得到方块坐标网格。"""
    tris = apply_display(tris, "fixed")
    out = []
    for t in tris:
        out.append([(((p[0]) * scale, (-p[2]) * scale, (p[1]) * scale), uv) for p, uv in t])
    return out


def main():
    parts = load_parts()
    cells = [
        ("旧 hold_ingot 侧立", "hold_ingot", "sb_hot_iron.png"),
        ("平放·铁", "hold_ingot_flat", "sb_hot_iron.png"),
        ("平放·钢", "hold_ingot_flat", "sb_hot_steel.png"),
        ("平放·融合钢", "hold_ingot_flat", "sb_hot_fused.png"),
    ]
    size = 170
    canvas = Image.new("RGBA", (size * len(cells), size), (24, 24, 30, 255))
    for i, (label, part, sheet) in enumerate(cells):
        tris = to_block(parts[part], 0.4)
        tex = Image.open(os.path.join(SHEET_DIR, sheet)).convert("RGBA")
        img = render([(tris, tex)], size=size, view="tp")
        canvas.alpha_composite(img, (i * size, 0))
    canvas.save(OUT)
    print("OK ->", OUT)


if __name__ == "__main__":
    main()
