# -*- coding: utf-8 -*-
"""
preview_textured.py —— 逐像素贴图渲染组装模型（模拟游戏内 clamp 采样），用于自查"拉伸感"。

与 preview_assembly.py（每面平均色）不同，本脚本按三角形 UV 从材质表逐像素采样，
和游戏内 AssemblyStageModel + entityCutoutNoCull（CLAMP_TO_EDGE、NEAREST）一致，
能真实暴露 UV 折叠/越界造成的拉丝。

用法:
  python3 animation_tools/preview_textured.py [帧列表，逗号分隔] [输出png]
  默认帧 42,99,120，输出 model-previews/assembly_textured.png
"""
import json
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

import assembly_vmd  # noqa: E402

NS = "slashbladeresh_slashblad"
JSON_PATH = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "assembly", "blade_parts.json")
TEX_PATH = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "assembly", "sb_wood.png")
PART_ORDER = ["sheath", "handle", "tsuba", "blade_blank"]  # 画序：先远后近（近似）


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


def _tilt(p, tilt_x=14.0, tilt_y=12.0, dist=2.6, zoom=900.0, w=1000, h=240):
    """复现游戏内变换：舞台点(x右,y上,z朝摄像机) -> pose 空间(x,y,-z)，
    先绕 Y 转 tilt_y、再绕 X 转 tilt_x（与 BladeAssemblyClient.drawStage 同序），
    最后透视投影。返回屏幕坐标与深度（越大越远）。"""
    x, y, z = p
    x, z = x, -z  # pose 空间: x 右不变、z 取反为"远离屏幕"
    cy, sy = math.cos(math.radians(tilt_y)), math.sin(math.radians(tilt_y))
    x, z = cy * x + sy * z, -sy * x + cy * z
    cx, sx = math.cos(math.radians(tilt_x)), math.sin(math.radians(tilt_x))
    y, z = cx * y - sx * z, sx * y + cx * z
    k = dist / (dist + z)
    return (w / 2 + x * zoom * k, h / 2 - y * zoom * k), z


def render(parts, tex, frame, tracks, w=1000, h=240, zoom=330.0, tilt=True):
    buf = [[(24, 24, 32)] * w for _ in range(h)]
    depth = [[-1e9] * w for _ in range(h)]
    tw, th = tex.size
    tp = tex.load()
    tris = []
    for name in PART_ORDER:
        dense = tracks[name]
        fx = min(frame, len(dense) - 1)
        _, (x, y, z), roll = dense[fx]
        cr, sr = math.cos(math.radians(roll)), math.sin(math.radians(roll))
        for (p0, u0, p1, u1, p2, u2) in parts[name]:
            pts = []
            zs = 0.0
            for (px, py, pz) in (p0, p1, p2):
                rx = px * cr - py * sr + x
                ry = px * sr + py * cr + y
                rz = pz + z
                if tilt:
                    (sx, sy_), zz = _tilt((rx, ry, rz), zoom=zoom * 1.6, w=w, h=h)
                    pts.append((sx, sy_)); zs += zz
                else:
                    pts.append((w / 2 + rx * zoom, h / 2 - ry * zoom))
                    zs += rz
            tris.append((zs / 3.0, pts, (u0, u1, u2)))
    tris.sort(key=lambda t: -t[0] if tilt else t[0])  # 倾斜模式：远(深度大)先画
    for _, pts, uvs in tris:
        (ax, ay), (bx, by), (cx, cy) = pts
        det = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        if abs(det) < 1e-6:
            continue
        shade = 0.72 + 0.28 * min(1.0, abs(det) / 1200.0)
        if det < 0:
            shade *= 0.88
        (u0, v0), (u1, v1), (u2, v2) = uvs
        x0 = max(0, int(min(ax, bx, cx))); x1 = min(w - 1, int(max(ax, bx, cx)) + 1)
        y0 = max(0, int(min(ay, by, cy))); y1 = min(h - 1, int(max(ay, by, cy)) + 1)
        # 标准重心坐标（点积法）：v0=C-A, v1=B-A, v2=P-A
        v0x, v0y = cx - ax, cy - ay
        v1x, v1y = bx - ax, by - ay
        den = v0x * v1y - v0y * v1x
        if abs(den) < 1e-9:
            continue
        for py in range(y0, y1 + 1):
            pyf = py + 0.5
            row_buf = buf[py]; row_dep = depth[py]
            for px in range(x0, x1 + 1):
                pxf = px + 0.5
                v2x, v2y = pxf - ax, pyf - ay
                lc = (v2x * v1y - v2y * v1x) / den   # C 权重
                lb = (v0x * v2y - v0y * v2x) / den   # B 权重
                la = 1.0 - lc - lb                    # A 权重
                l1, l2, l3 = la, lb, lc
                if l1 < 0 or l2 < 0 or l3 < 0:
                    continue
                u = l1 * u0 + l2 * u1 + l3 * u2
                v = l1 * v0 + l2 * v1 + l3 * v2
                tx = min(tw - 1, max(0, int(u * tw)))
                ty = min(th - 1, max(0, int(v * th)))
                r, g, b, a = tp[tx, ty]
                if a < 128:
                    continue
                row_dep[px] = 0  # 近者覆盖：已按 z 排序，直接画
                row_buf[px] = (int(r * shade), int(g * shade), int(b * shade))
    img = Image.new("RGB", (w, h))
    img.putdata([c for row in buf for c in row])
    return img


def main():
    frames = [int(s) for s in (sys.argv[1].split(",") if len(sys.argv) > 1 else ["42", "99", "120"])]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "model-previews", "assembly_textured.png")
    parts, tex = load()
    tracks = {name: dense for name, dense in assembly_vmd.build_stage_tracks()}
    imgs = [render(parts, tex, f, tracks) for f in frames]
    sheet = Image.new("RGB", (imgs[0].width, imgs[0].height * len(imgs) + 4 * (len(imgs) - 1)), (8, 8, 12))
    for i, im in enumerate(imgs):
        sheet.paste(im, (0, i * (im.height + 4)))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
    print("OK", out, sheet.size)


if __name__ == "__main__":
    main()
