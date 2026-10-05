# -*- coding: utf-8 -*-
"""从寒霜混合网格切出组装动画四部件（blade_blank/tsuba/handle/sheath）。

刀身、刀镡与刀柄来自寒霜原有村正网格，刀鞘与下绪来自付丧使用的 agito 网格。

与 build_parts_model.py 同一套舞台坐标约定：
  原点 = 刀镡中心，X 沿刀身、刀尖朝 -X，Y 屏幕上为正，Z 朝摄像机，单位 = 格。
输出：
  assembly/frost_parts.json —— 与 blade_parts.json 同结构，客户端按部件名取网格
  textures/assembly/sb_frost.png —— 部件材质表（frost.png 的副本，UV 原样，无需平铺）
用法: python3 animation_tools/build_frost_parts_model.py
"""
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FROST_DIR = ROOT / "src" / "main" / "resources" / "assets" / "slashbladeresh_slashblad" / "model" / "named" / "frost"
RES = ROOT / "src" / "main" / "resources" / "assets" / "slashbladeresh_slashblad"
OUT_JSON = RES / "assembly" / "frost_parts.json"
OUT_TEX = RES / "textures" / "assembly" / "sb_frost.png"

# 与木偶/竹光组装动画同一约定（build_parts_model.py）：原点放在刀镡中心附近
ORIGIN_X, ORIGIN_Y = -30.0, 3.0
SCALE = 1.18 / 345.0

PART_ORDER = ["blade_blank", "tsuba", "handle", "sheath"]


def load_obj(path):
    vs, vts = [], []
    group = None
    faces = []
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            vs.append(tuple(map(float, t[1:4])))
        elif t[0] == "vt":
            vts.append(tuple(map(float, t[1:3])))
        elif t[0] == "g":
            group = t[1]
        elif t[0] == "f" and group:
            vi = [int(x.split("/")[0]) for x in t[1:4]]
            ti = [int(x.split("/")[1]) for x in t[1:4]]
            faces.append((group, vi, ti))
    return vs, vts, faces


def norm(mx, my, mz):
    return ((mx - ORIGIN_X) * SCALE, -(my - ORIGIN_Y) * SCALE, mz * SCALE)


def classify(group, vs, vi):
    if group == "sheath":
        return "sheath"
    if group == "handle":
        return "handle"  # 村正真实缠柄分组，随刀柄骨骼一起推上
    if group == "blade":
        pts = [vs[i - 1] for i in vi]
        cx = sum(p[0] for p in pts) / 3.0
        zmax = max(abs(p[2]) for p in pts)
        # 新雪花刀镡的每个面都落在固定的薄层 x=-38.05..-35.15；中心主枝
        # 的 z 很小，不能再沿用村正刀镡的 zmax 判定，否则组装展示会漏掉枝条。
        if all(-38.1 <= p[0] <= -35.1 for p in pts):
            return "tsuba"
        if -45.0 <= cx < -15.0 and zmax >= 4.0:
            return "tsuba"
        # 村正 blade 分组里 cx >= -15 的后段其实是刀柄的重复网格
        # （UV 落在缠柄花纹区），留在刀条上会让刀尾看着像刀柄。
        # 按要求删掉，改用下方合成的"茎"（刀条尾）——与木偶/竹光同款。
        if cx >= -15.0:
            return None
        return "blade_blank"
    return None  # 其余分组（item_*、effect 等）不进组装舞台


def root_uv_rect(vs, vts, faces):
    """返回寒霜图集中确定为银灰钢面的安全小块，避免刀茎误采到村正金色装饰。"""
    # frost.png 左半仍是 256×512 的寒霜刀身区；(230, 400) 位于未装饰的钢面。
    cu, cv = 230.0 / 512.0, 400.0 / 512.0
    return (cu - 0.004, cv - 0.004, cu + 0.004, cv + 0.004)


# 薄盒 12 三角的顶点索引表（与 build_parts_model.py 的 _BOX_TRIS 相同）
_BOX_TRIS = [(0, 2, 3), (0, 3, 1), (4, 5, 7), (4, 7, 6), (0, 1, 5), (0, 5, 4),
             (2, 6, 7), (2, 7, 3), (1, 3, 7), (1, 7, 5), (0, 4, 6), (0, 6, 2)]


def tang_box(uv_rect):
    """刀条尾部合成的那截"茎"：从刀镡内穿到柄下，薄而扁，取根部钢色。

    尺寸贴着村正刀根断面（y -2.2..8.8、z ±1.3）略收一圈；尾端伸到
    舞台 x≈0.15 格，与木偶/竹光的刀尾长度一致，之后被刀柄罩住。"""
    x0, x1, y0, y1, z0, z1 = -40.0, 14.0, -0.6, 7.2, -1.1, 1.1
    p = [norm(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    # p 索引: (x0y0z0)=0 (x0y0z1)=1 (x0y1z0)=2 (x0y1z1)=3 (x1y0z0)=4 (x1y0z1)=5 (x1y1z0)=6 (x1y1z1)=7
    u0, v0, u1, v1 = uv_rect
    uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
    out = []
    for k, tri in enumerate(_BOX_TRIS):
        uv = uvs[k % 4]
        for vi in tri:
            out.append((p[vi], uv))
    return out


def main():
    vs, vts, faces = load_obj(FROST_DIR / "frost.obj")
    parts = {name: [] for name in PART_ORDER}
    for group, vi, ti in faces:
        part = classify(group, vs, vi)
        if part is None:
            continue
        pts = [vs[i - 1] for i in vi]
        uvs = [vts[i - 1] for i in ti]
        for k in range(3):
            # v 翻转到游戏 UV（与木偶部件同一处理）；u 原样（村正表无横向平铺）
            parts[part].append((norm(*pts[k]), (uvs[k][0], 1.0 - uvs[k][1])))
    # 刀尾"茎"：木偶/竹光同款的薄盒，取根部钢色，替代被删掉的柄形重复网格
    parts["blade_blank"].extend(tang_box(root_uv_rect(vs, vts, faces)))
    doc = {
        "note": "名刀·寒霜组装部件网格（寒霜刀身 + 付丧刀鞘，舞台坐标与 blade_parts.json 一致）；"
                "材质表 textures/assembly/sb_frost.png。",
        "parts": {name: [round(v, 6) for p, uv in parts[name] for v in (*p, *uv)]
                  for name in PART_ORDER},
    }
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(doc), encoding="utf-8")
    OUT_TEX.parent.mkdir(parents=True, exist_ok=True)
    OUT_TEX.write_bytes((FROST_DIR / "frost.png").read_bytes())
    for name in PART_ORDER:
        print(f"  {name:12s} {len(parts[name]) // 3:4d} tris")
    print("输出:", OUT_JSON.relative_to(ROOT), OUT_TEX.relative_to(ROOT))


if __name__ == "__main__":
    main()
