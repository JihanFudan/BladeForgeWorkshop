# -*- coding: utf-8 -*-
"""渲染名刀·寒霜 OBJ + frost.png 的多视角预览，用于贴图与整体观感自查。"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "model-previews" / "frost_blade_preview.png"
OBJ = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.obj"
TEX = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.png"


def load(keep=None):
    vs, vts = [], []
    tris = []
    lines = OBJ.read_text(encoding="utf-8", errors="replace").splitlines()
    keep = keep or {"blade", "handle", "sheath", "effect"}
    group = None
    for line in lines:
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            vs.append(tuple(map(float, t[1:4])))
        elif t[0] == "vt":
            vts.append(tuple(map(float, t[1:3])))
        elif t[0] == "g":
            group = t[1]
        elif t[0] == "f" and (group is None or group in keep):
            vi = [int(x.split("/")[0]) for x in t[1:4]]
            ti = [int(x.split("/")[1]) for x in t[1:4]]
            tris.append((vi, ti))
    return vs, vts, tris


def rot(p, yaw, pitch, roll=0.0):
    x, y, z = p
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = cy * x - sy * z, sy * x + cy * z
    cx, sx = math.cos(pitch), math.sin(pitch)
    y, z = cx * y - sx * z, sx * y + cx * z
    cz, sz = math.cos(roll), math.sin(roll)
    x, y = cz * x - sz * y, sz * x + cz * y
    return x, y, z


def render(vs, vts, tris, tex, yaw, pitch, roll, size=(430, 320)):
    W, H = size
    img = Image.new("RGBA", (W, H), (24, 25, 31, 255))
    depth = [[1e18] * W for _ in range(H)]
    pts_all = [rot(vs[i - 1], yaw, pitch, roll) for vi, _ in tris for i in vi]
    xs = [p[0] for p in pts_all]; ys = [p[1] for p in pts_all]; zs = [p[2] for p in pts_all]
    cx, cy, cz = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, 0.0
    ext = max(max(xs) - min(xs), max(ys) - min(ys), 1e-3)
    sc = min(W, H) * 0.9 / ext
    light = rot((-0.35, 0.55, 0.76), yaw, pitch, roll)
    ln = math.sqrt(sum(v * v for v in light)) or 1.0
    light = tuple(v / ln for v in light)
    texpx = tex.load()
    tw, th = tex.size
    data = []
    for vi, ti in tris:
        pts = [rot(vs[i - 1], yaw, pitch, roll) for i in vi]
        (ax, ay, az), (bx, by, bz), (gx, gy, gz) = pts
        nx = (by - ay) * (gz - by) - (bz - by) * (gy - ay)
        ny = (bz - az) * (gx - ax) - (bx - ax) * (gz - az)
        nz = (bx - ax) * (gy - ay) - (by - ay) * (gx - ax)
        nl = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
        facing = -nz / nl
        if facing <= 0.01:
            continue  # 背面剔除
        bright = 0.36 + 0.72 * max(0.0, (nx * light[0] + ny * light[1] + nz * light[2]) / nl)
        bright = min(1.25, bright + 0.18 * facing)
        zavg = (az + bz + gz) / 3
        uvs = [((vts[i - 1][0] * tw), ((1.0 - vts[i - 1][1]) * th)) for i in ti]
        scr = [((px - cx) * sc + W / 2, -(py - cy) * sc + H / 2) for px, py, pz in pts]
        data.append((zavg, scr, [az, bz, gz], uvs, bright))
    data.sort(key=lambda x: x[0], reverse=True)
    for zavg, scr, zvals, uvs, bright in data:
        x1, y1 = scr[0]; x2, y2 = scr[1]; x3, y3 = scr[2]
        minx = max(0, int(min(x1, x2, x3))); maxx = min(W - 1, int(max(x1, x2, x3)) + 1)
        miny = max(0, int(min(y1, y2, y3))); maxy = min(H - 1, int(max(y1, y2, y3)) + 1)
        denom = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        if abs(denom) < 1e-9:
            continue
        for yy in range(miny, maxy + 1):
            for xx in range(minx, maxx + 1):
                a = ((y2 - y3) * (xx - x3) + (x3 - x2) * (yy - y3)) / denom
                b = ((y3 - y1) * (xx - x3) + (x1 - x3) * (yy - y3)) / denom
                c = 1 - a - b
                if a < -1e-6 or b < -1e-6 or c < -1e-6:
                    continue
                zpix = a * zvals[0] + b * zvals[1] + c * zvals[2]
                if zpix >= depth[yy][xx]:
                    continue
                uu = int(max(0.0, min(0.999999, a * (uvs[0][0] / tw) + b * (uvs[1][0] / tw) + c * (uvs[2][0] / tw))) * tw)
                vv = int(max(0.0, min(0.999999, a * (uvs[0][1] / th) + b * (uvs[1][1] / th) + c * (uvs[2][1] / th))) * th)
                r, g, bl, alpha = texpx[uu, vv]
                if alpha < 128:
                    continue
                depth[yy][xx] = zpix
                r = min(255, int(r * bright)); g = min(255, int(g * bright)); bl = min(255, int(bl * bright))
                img.putpixel((xx, yy), (r, g, bl, 255))
    return img


def main():
    vs, vts, tris = load()
    tex = Image.open(TEX).convert("RGBA")
    views = [
        ("front", -1.05, -0.18, 0.79),
        ("back", 2.09, 0.18, -0.79),
        ("side-up", -0.35, 0.72, 0.4),
        ("top", 0.2, 1.22, 0.2),
    ]
    W, H = 880, 660
    canvas = Image.new("RGB", (W, H), (15, 15, 20))
    d = ImageDraw.Draw(canvas)
    for idx, (name, yaw, pitch, roll) in enumerate(views):
        x = (idx % 2) * 440 + 5
        y = (idx // 2) * 330 + 5
        canvas.paste(render(vs, vts, tris, tex, yaw, pitch, roll, (430, 320)), (x, y))
        d.text((x + 3, y + 2), name, fill=(220, 238, 255))
    OUT.parent.mkdir(exist_ok=True)
    canvas.save(OUT)

    # 单独模拟物品栏模型。必须看到刀与鞘分离并十字交叉，且不能出现覆盖格子的整块方片。
    item_vs, item_vts, item_tris = load({"item_blade"})
    inventory = render(item_vs, item_vts, item_tris, tex, 0.0, 0.0, 0.0, (512, 384))
    inventory.save(ROOT / "model-previews" / "frost_inventory_icon.png")

    # 沿刀身方向正对刀镡渲染近景，确认六向雪花轮廓与金色材质都确实生效。
    tsuba_tris = []
    for vi, ti in load({"blade"})[2]:
        points = [vs[index - 1] for index in vi]
        if all(-38.1 <= point[0] <= -35.1 for point in points):
            tsuba_tris.append((vi, ti))
    closeup = render(vs, vts, tsuba_tris, tex, math.pi / 2.0, 0.0, 0.0, (512, 512))
    closeup.save(ROOT / "model-previews" / "frost_tsuba_closeup.png")
    print(OUT)


if __name__ == "__main__":
    main()
