# -*- coding: utf-8 -*-
"""
preview_held_models.py —— 手持物品模型自查渲染。

按原版那条真实的变换链把 hold_* 部件摆到三种视角下画出来：

  1) 背包格子：display.gui（未写 = 单位变换）+ 正交前视 —— 用来确认
     "如果背包也用 3D 网格会长成一条线"，所以背包保留平面图标；
  2) 第一人称手上：display.firstperson_righthand + applyItemArmTransform
     的平移 (0.56, -0.52, -0.72) + 透视投影（相机在原点朝 -Z）；
  3) 第三人称手上：display.thirdperson_righthand + ItemInHandLayer 的
     Rx(-90)·Ry(180) + translate(1/16, 0.125, -0.625)，相机在人物右后方
     45° 斜视（正后方看是侧缝，原版剑也一样）。

ItemTransform 的实现照抄 net.minecraft.client.renderer.block.model.ItemTransform#apply：
  v -> T(translation/16) · Rx(rx) · Ry(ry) · Rz(rz) · S(scale) · (v - 0.5)

用法: python3 animation_tools/preview_held_models.py
输出: model-previews/held_models_check.png
"""
import json
import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
RES = os.path.join(ROOT, "src", "main", "resources", "assets", NS)
PARTS = os.path.join(RES, "assembly", "blade_parts.json")
ICON_DIR = os.path.join(RES, "textures", "item")
SHEET_DIR = os.path.join(RES, "textures", "assembly")
OUT = os.path.join(ROOT, "model-previews", "held_models_check.png")

# 与 models/item/<name>.json 里的 display 段保持一致。
# 刀条网格的方位角从 45° 抬到 102°（刀尖在手里向前倾），屏幕上的斜度只由 rz 决定，
# 所以 ground/fixed 的 rz 同步减掉 57°、第三人称减掉 55°，摆放观感和改之前一样。
DISPLAY = {
    "gui": (0, 0, 0, 0, 0, 0, 1, 1, 1),
    "ground": (0, 0, -57, 0, 2, 0, 0.9, 0.9, 0.9),
    "fixed": (0, 180, 57, 0, 0, 0, 1.5, 1.5, 1.5),
    "thirdperson": (0, -90, 0, 0, 4.0, 0.5, 1.6, 1.6, 1.6),
    "firstperson": (0, -90, 25, 1.13, 3.2, 1.13, 2.2, 2.2, 2.2),
}

# 每行：标签 + 若干层（部件名, 材质表）。刀条那几行两层＝钳子 + 工件。
ROWS = [
    ("刀鞘", [("hold_sheath", "sb_wood.png")], "blade_sheath.png"),
    ("成品刀条", [("hold_blade_metal", "sb_oodachi.png")], "quenched_blade.png"),
    ("木刀条", [("hold_blade_wood", "sb_wood.png")], "wooden_blade_blank.png"),
    ("竹刀条", [("hold_blade_bamboo", "sb_bamboo.png")], "bamboo_blade_blank.png"),
    ("钳子夹刀条", [("hold_blade_grip", "sb_hot_blade.png"),
                ("hold_tongs_blade", "sb_iron.png")], "hot_blade.png"),
    ("灼热锭子", [("hold_ingot", "sb_hot_iron.png")], "heated_iron.png"),
    ("灼热钢锭", [("hold_ingot", "sb_hot_steel.png")], "heated_steel.png"),
    ("灼热低碳钢", [("hold_ingot", "sb_hot_lowcarbon.png")], "heated_low_carbon.png"),
    ("灼热高碳钢", [("hold_ingot", "sb_hot_highcarbon.png")], "heated_high_carbon.png"),
    ("钳夹低碳钢", [("hold_ingot_grip", "sb_hot_lowcarbon.png"),
                ("hold_tongs_ingot", "sb_iron.png")], "heated_low_carbon.png"),
    ("钳夹高碳钢", [("hold_ingot_grip", "sb_hot_highcarbon.png"),
                ("hold_tongs_ingot", "sb_iron.png")], "heated_high_carbon.png"),
    ("钳子夹锭子", [("hold_ingot_grip", "sb_hot_iron.png"),
                ("hold_tongs_ingot", "sb_iron.png")], "heated_steel.png"),
]

CELL = 150
PAD = 10


def load_parts():
    doc = json.load(open(PARTS, encoding="utf-8"))
    out = {}
    for name, arr in doc["parts"].items():
        tris = []
        for i in range(0, len(arr) - 14, 15):
            tris.append(tuple(((arr[i + k * 5], arr[i + k * 5 + 1], arr[i + k * 5 + 2]),
                               (arr[i + k * 5 + 3], arr[i + k * 5 + 4])) for k in range(3)))
            tris[-1] = list(tris[-1])
        out[name] = tris
    return out


def _rot_x(p, a):
    x, y, z = p
    c, s = math.cos(a), math.sin(a)
    return (x, y * c - z * s, y * s + z * c)


def _rot_y(p, a):
    x, y, z = p
    c, s = math.cos(a), math.sin(a)
    return (x * c + z * s, y, -x * s + z * c)


def _rot_z(p, a):
    x, y, z = p
    c, s = math.cos(a), math.sin(a)
    return (x * c - y * s, x * s + y * c, z)


def apply_display(tris, key):
    rx, ry, rz, tx, ty, tz, sx, sy, sz = DISPLAY[key]
    rx, ry, rz = math.radians(rx), math.radians(ry), math.radians(rz)

    def one(p):
        q = ((p[0] - 0.5) * sx, (p[1] - 0.5) * sy, (p[2] - 0.5) * sz)
        # 原版 ItemTransform#apply 用的是 Quaternionf.rotationXYZ(rx,ry,rz)，
        # 等价于矩阵 Rz·Ry·Rx —— 作用到顶点上是"先绕 X、再绕 Y、最后绕 Z"。
        q = _rot_x(q, rx)
        q = _rot_y(q, ry)
        q = _rot_z(q, rz)
        return (q[0] + tx / 16.0, q[1] + ty / 16.0, q[2] + tz / 16.0)

    return [[(one(p), uv) for p, uv in t] for t in tris]


def apply_hand(tris, first_person):
    """第一人称：只平移；第三人称：Rx(-90)·Ry(180) 再平移。"""
    def one(p):
        q = p
        if not first_person:
            q = _rot_y(q, math.radians(180.0))
            q = _rot_x(q, math.radians(-90.0))
            return (q[0] + 1.0 / 16.0, q[1] + 0.125, q[2] - 0.625)
        return (q[0] + 0.56, q[1] - 0.52, q[2] - 0.72)

    return [[(one(p), uv) for p, uv in t] for t in tris]


def centroid(tris):
    n = sum(len(t) for t in tris)
    return tuple(sum(p[0][i] for t in tris for p in t) / n for i in range(3))


def render(layers, size=CELL, view="gui", bg=(30, 30, 38, 255)):
    """view: gui=正交前视, fp=第一人称透视, tp=第三人称右后 45° 斜视, frame=展示框。

    layers 是 [(三角表, 材质表), ...]——多层（钳子 + 工件）必须画进同一张图里
    统一按深度排序，否则后画的一层会把先画的一层整块盖掉。"""
    im = Image.new("RGBA", (size, size), bg)
    d = ImageDraw.Draw(im)
    allp = [p for tris, _ in layers for t in tris for p, _ in t]
    c = tuple(sum(p[i] for p in allp) / len(allp) for i in range(3))
    if view == "gui":
        eye = (c[0], c[1], c[2] + 4.0)
        fov, ortho = 1.0, 1.35
    elif view == "fp":
        eye = (0.0, 0.0, 0.0)
        fov, ortho = 0.0, 1.0
    elif view == "frame":
        eye = (c[0], c[1], c[2] + 3.0)
        fov, ortho = 1.0, 1.7
    else:
        eye = (c[0] + 2.1, c[1] + 1.5, c[2] + 2.1)
        fov, ortho = 0.0, 1.0
    # 相机基向量：看向目标（gui/frame 就是正前视）
    tx, ty, tz = (c[0] - eye[0], c[1] - eye[1], c[2] - eye[2])
    if view in ("gui", "frame"):
        fwd = (0.0, 0.0, -1.0)
    else:
        L = math.sqrt(tx * tx + ty * ty + tz * tz)
        fwd = (tx / L, ty / L, tz / L)
    up0 = (0.0, 1.0, 0.0)
    right = (fwd[1] * up0[2] - fwd[2] * up0[1], fwd[2] * up0[0] - fwd[0] * up0[2],
             fwd[0] * up0[1] - fwd[1] * up0[0])
    RL = math.sqrt(sum(v * v for v in right)) or 1.0
    right = tuple(v / RL for v in right)
    up = (right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2],
          right[0] * fwd[1] - right[1] * fwd[0])
    order = []
    for tris, tex in layers:
        px = tex.load()
        tw, th = tex.size
        for t in tris:
            pts = []
            ds = []
            for (x, y, z), uv in t:
                vx, vy, vz = x - eye[0], y - eye[1], z - eye[2]
                cam = (vx * right[0] + vy * right[1] + vz * right[2],
                       vx * up[0] + vy * up[1] + vz * up[2],
                       vx * fwd[0] + vy * fwd[1] + vz * fwd[2])
                ds.append(cam[2])
                if ortho:
                    sc = size / ortho
                    pts.append((size / 2 + cam[0] * sc, size / 2 - cam[1] * sc))
                else:
                    depth = max(cam[2], 0.05)
                    sc = size * fov / depth
                    pts.append((size / 2 + cam[0] * sc, size / 2 - cam[1] * sc))
            order.append((sum(ds) / 3.0, pts, t, px, tw, th))
    order.sort(key=lambda e: -e[0])
    for _, scr, t, px, tw, th in order:
        rs = gs = bs = n = 0
        for wa, wb, wc in ((1 / 3, 1 / 3, 1 / 3), (0.8, 0.1, 0.1), (0.1, 0.8, 0.1), (0.1, 0.1, 0.8)):
            p = [t[0][1][i] * wa + t[1][1][i] * wb + t[2][1][i] * wc for i in range(2)]
            u = min(max(p[0], 0.0), 0.9999) * tw
            v = min(max(p[1], 0.0), 0.9999) * th
            r, g, b, a = px[int(u), int(v)]
            if a < 128:
                continue
            rs += r; gs += g; bs += b; n += 1
        if n:
            d.polygon(scr, fill=(rs // n, gs // n, bs // n, 255))
    return im


def main():
    parts = load_parts()
    sheets = sorted({s for _, layers, _ in ROWS for _, s in layers})
    tiled = {n: Image.open(os.path.join(SHEET_DIR, n)).convert("RGBA") for n in sheets}
    cols = ["平面图标(背包)", "3D 网格(背包位)", "第一人称手上", "第三人称手上", "展示框"]
    canvas = Image.new("RGBA", (PAD + (CELL + PAD) * len(cols) + 90,
                                PAD + (CELL + PAD * 2 + 22) * len(ROWS) + PAD), (16, 16, 20, 255))
    d = ImageDraw.Draw(canvas)
    for i, c in enumerate(cols):
        d.text((90 + PAD + i * (CELL + PAD) + 4, 4), c, fill=(235, 235, 235, 255))

    def stack(layers, fn=None):
        return [(fn(parts[p]) if fn else parts[p], tiled[s]) for p, s in layers]

    y = 22
    for label, layers, icon in ROWS:
        d.text((4, y + CELL / 2 - 6), label, fill=(240, 240, 240, 255))
        x = PAD + 90
        canvas.alpha_composite(Image.open(os.path.join(ICON_DIR, icon)).convert("RGBA")
                               .resize((CELL, CELL), Image.NEAREST), (x, y))
        x += CELL + PAD
        canvas.alpha_composite(render(stack(layers), view="gui"), (x, y))
        x += CELL + PAD
        canvas.alpha_composite(render(stack(layers, lambda t: apply_hand(apply_display(t, "firstperson"), True)),
                                      view="fp"), (x, y))
        x += CELL + PAD
        canvas.alpha_composite(render(stack(layers, lambda t: apply_hand(apply_display(t, "thirdperson"), False)),
                                      view="tp"), (x, y))
        x += CELL + PAD
        canvas.alpha_composite(render(stack(layers, lambda t: apply_display(t, "fixed")), view="frame"), (x, y))
        y += CELL + PAD * 2 + 22
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    canvas.convert("RGB").save(OUT)
    print("OK ->", OUT, canvas.size)


if __name__ == "__main__":
    main()
