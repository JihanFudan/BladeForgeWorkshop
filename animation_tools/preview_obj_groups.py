# -*- coding: utf-8 -*-
"""
preview_obj_groups.py —— 把拔刀剑本体 jar 里任意 OBJ 的任意分组带贴图渲染成预览图。

用途：确认"大太刀 / 木偶 / 竹光"各自的刀刃(blade)与刀鞘(sheath)长什么样，
再决定手持部件用哪一份网格。纯标准库 + Pillow，画家算法（按面深度排序）。

用法: python3 animation_tools/preview_obj_groups.py [输出png]
"""
import io
import math
import os
import sys
import zipfile

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(ROOT, "libs", "SlashBladeResharped-2.0.5-1.21.1.jar")
BASE = "assets/slashblade/"

CANDIDATES = [
    ("blade.obj 太刀 + wood.png(木偶)", "model/blade.obj", "model/wood.png"),
    ("blade.obj 太刀 + white.png(白鞘)", "model/blade.obj", "model/white.png"),
    ("yamato.obj 阎魔刀", "model/named/yamato.obj", "model/named/yamato.png"),
    ("dios.obj 枯石大刀", "model/named/dios/dios.obj", "model/named/dios/koseki.png"),
    ("sange.obj 散华", "model/named/sange/sange.obj", "model/named/sange/sange.png"),
    ("agito.obj 鄂门", "model/named/agito.obj", "model/named/agito_false.png"),
]
GROUPS = ["blade", "sheath", "item_blade"]


def load_obj(zf, path):
    vs, vts, g = [], [], None
    faces = {}
    for line in zf.read(BASE + path).decode("utf-8", errors="replace").splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            vs.append((float(t[1]), float(t[2]), float(t[3])))
        elif t[0] == "vt":
            vts.append((float(t[1]), float(t[2])))
        elif t[0] == "g":
            g = t[1]
            faces.setdefault(g, [])
        elif t[0] == "f" and g:
            faces[g].append(([int(x.split("/")[0]) for x in t[1:4]],
                             [int(x.split("/")[1]) for x in t[1:4]]))
    return vs, vts, faces


def render_group(tris, tex, size=240, rot_y=30.0, rot_x=12.0):
    if not tris:
        im = Image.new("RGBA", (size, size), (34, 34, 40, 255))
        return im
    allp = [p for t in tris for p in t]
    cx = sum(v[0][0] for v in allp) / len(allp)
    cy = sum(v[0][1] for v in allp) / len(allp)
    cz = sum(v[0][2] for v in allp) / len(allp)
    xs = [v[0][0] for v in allp]
    ys = [v[0][1] for v in allp]
    span = max(max(xs) - min(xs), max(ys) - min(ys))
    scale = (size * 0.88) / span
    a, b = math.radians(rot_y), math.radians(rot_x)
    ca, sa, cb, sb = math.cos(a), math.sin(a), math.cos(b), math.sin(b)

    def proj(p):
        x, y, z = p[0] - cx, p[1] - cy, p[2] - cz
        x2, z2 = ca * x + sa * z, -sa * x + ca * z
        y2, z3 = cb * y - sb * z2, sb * y + cb * z2
        return (size / 2 + x2 * scale, size / 2 - y2 * scale, z3)

    im = Image.new("RGBA", (size, size), (34, 34, 40, 255))
    d = ImageDraw.Draw(im)
    tp = tex.load()
    tw, th = tex.size
    order = []
    for tri in tris:
        pz = 0.0
        for p, uv in tri:
            pz += proj(p)[2]
        order.append((pz / 3.0, tri))
    order.sort(key=lambda e: e[0])       # 远的先画
    for _, tri in order:
        pts, uvs = [], []
        for p, uv in tri:
            sx, sy, _ = proj(p)
            pts.append((sx, sy))
            u = uv[0] % 1.0
            v = 1.0 - uv[1]
            uvs.append((min(tw - 1, max(0, int(u * tw))), min(th - 1, max(0, int(v * th)))))
        if uvs[0] == uvs[1] == uvs[2]:
            col = tp[uvs[0][0], uvs[0][1]]
        else:
            rs = gs = bs = ns = 0
            for (ux, uy) in uvs:
                c = tp[ux, uy]
                if c[3] >= 128:
                    rs += c[0]; gs += c[1]; bs += c[2]; ns += 1
            if ns == 0:
                continue
            col = (rs // ns, gs // ns, bs // ns, 255)
        if col[3] < 128:
            continue
        d.polygon(pts, fill=col)
    return im


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "model-previews/sb_models_compare.png"
    zf = zipfile.ZipFile(JAR)
    pad, size = 8, 240
    rows = []
    for label, obj, tex in CANDIDATES:
        vs, vts, faces = load_obj(zf, obj)
        image = Image.open(io.BytesIO(zf.read(BASE + tex))).convert("RGBA")
        cells = []
        for grp in GROUPS:
            tris = []
            for vi, ti in faces.get(grp, []):
                tris.append([(vs[vi[k] - 1], vts[ti[k] - 1]) for k in range(3)])
            cells.append(render_group(tris, image, size=size))
        rows.append((label, cells))
    W = pad + (size + pad) * len(GROUPS)
    H = pad + (size + 26 + pad) * len(rows)
    canvas = Image.new("RGBA", (W, H), (20, 20, 24, 255))
    d = ImageDraw.Draw(canvas)
    y = pad
    for label, cells in rows:
        d.text((pad + 2, y), "%s   |   %s" % (label, "  ".join(GROUPS)), fill=(235, 235, 235, 255))
        y += 22
        for i, c in enumerate(cells):
            canvas.alpha_composite(c, (pad + i * (size + pad), y))
        y += size + pad * 2
    op = os.path.join(ROOT, out)
    os.makedirs(os.path.dirname(op), exist_ok=True)
    canvas.convert("RGB").save(op)
    print("->", out, canvas.size)


if __name__ == "__main__":
    main()
