# -*- coding: utf-8 -*-
"""按 CustomBladeRenderer 的真实拆件与物品栏变换渲染寒霜自定义刀预览。"""
from pathlib import Path
from PIL import Image, ImageDraw

import preview_frost_blade as preview

ROOT = Path(__file__).resolve().parent.parent
OBJ = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.obj"
TEX = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.png"
OUT = ROOT / "model-previews/custom_blade_default_layout.png"
BLADE_END = -42.4841
BLADE_ROOT_INSERT_MAX_X = -34.0
BLADE_ROOT_MAX_ABS_Z = 1.5
GUARD_MIN_X = -45.0
GUARD_MAX_X = -15.0
GUARD_MIN_RADIAL_Z = 4.0
HANDLE_START = -32.7929
HANDLE_SEAM_OVERLAP = 3.5
HANDLE_SEAM_MAX_ABS_Z = 3.75
HANDLE_SEAM_MIN_Y = -3.75
HANDLE_SEAM_MAX_Y = 10.0


def load_parts():
    vertices, uvs, grouped = [], [], {}
    group = ""
    for line in OBJ.read_text(encoding="utf-8").splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            vertices.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "vt":
            uvs.append(tuple(map(float, fields[1:3])))
        elif fields[0] == "g":
            group = fields[1].lower()
        elif fields[0] == "f":
            vi = tuple(int(token.split("/")[0]) for token in fields[1:4])
            ti = tuple(int(token.split("/")[1]) for token in fields[1:4])
            grouped.setdefault(group, []).append((vi, ti))

    def bounds(face):
        points = [vertices[index - 1] for index in face[0]]
        return (min(p[0] for p in points), max(p[0] for p in points),
                sum(p[0] for p in points) / len(points), max(abs(p[2]) for p in points),
                min(p[1] for p in points), max(p[1] for p in points))

    raw_blade = grouped.get("blade", [])
    def blade_root_insert(face):
        q = bounds(face)
        return (q[0] >= BLADE_END - 0.1 and q[1] <= BLADE_ROOT_INSERT_MAX_X
                and q[3] <= BLADE_ROOT_MAX_ABS_Z and q[4] >= -3.0 and q[5] <= 9.5)

    # 保留刀根钢芯继续插进刀镡的窄面；横向装饰与旧套环仍然剔除。
    blade = [face for face in raw_blade
             if bounds(face)[0] < BLADE_END - 0.75 or blade_root_insert(face)]
    guard = list(grouped.get("guard", []))
    if guard:
        # 寒霜独立 guard 分组缺少中轴刀簇，补入同一薄层内的中心套口面。
        guard.extend(face for face in raw_blade
                     if bounds(face)[0] >= -38.1 and bounds(face)[1] <= -35.1
                     and bounds(face)[3] <= GUARD_MIN_RADIAL_Z
                     and bounds(face)[4] >= -7.0 and bounds(face)[5] <= 14.0
                     and not blade_root_insert(face))
    else:
        guard = [face for face in raw_blade
                 if GUARD_MIN_X <= bounds(face)[2] < GUARD_MAX_X
                 and (bounds(face)[3] >= GUARD_MIN_RADIAL_Z
                      or bounds(face)[4] <= -7.0 or bounds(face)[5] >= 14.0)]
    raw_handle = grouped.get("handle", []) or [
        face for face in raw_blade
        if bounds(face)[1] >= HANDLE_START
        and bounds(face)[0] >= HANDLE_START - HANDLE_SEAM_OVERLAP
    ]
    # 跨过刀柄起点且完整落在紧凑柄首截面内的面整圈保留，补齐上下、左右四周；
    # 横向展开的刀镡与完全停在刀镡一侧的旧端盖仍然不会进入结果。
    handle = [
        face for face in raw_handle
        if ((bounds(face)[0] >= HANDLE_START - 0.05)
            or (bounds(face)[1] >= HANDLE_START - HANDLE_SEAM_OVERLAP
                and bounds(face)[0] >= HANDLE_START - HANDLE_SEAM_OVERLAP * 2.0
                and bounds(face)[1] - bounds(face)[0] <= HANDLE_SEAM_OVERLAP + 0.25
                and bounds(face)[3] <= HANDLE_SEAM_MAX_ABS_Z
                and bounds(face)[4] >= HANDLE_SEAM_MIN_Y
                and bounds(face)[5] <= HANDLE_SEAM_MAX_Y))
        and not (GUARD_MIN_X <= bounds(face)[2] < GUARD_MAX_X
                 and (bounds(face)[3] >= GUARD_MIN_RADIAL_Z
                      or bounds(face)[4] <= -7.0 or bounds(face)[5] >= 14.0))
    ]
    sheath = grouped.get("sheath", [])
    return vertices, uvs, {"blade": blade, "guard": guard, "handle": handle, "sheath": sheath}


def blade_transform(point):
    x, y, z = point
    return (-0.3513422091 * x - 1.2292924936 * y + 0.0000086237 * z - 63.2631436785,
            0.4696877503 * x - 0.8699876138 * y - 0.0000101678 * z + 57.8811116816,
            1.4039793118 * z + 15.593)


def sheath_transform(point):
    x, y, z = point
    return (0.3425939586 * x + 0.77083591 * y - 0.0000083467 * z + 63.8568170358,
            0.4477656188 * x - 0.6248078361 * y - 0.0000066559 * z + 62.4343619704,
            -0.6393679184 * z + 8.4590103634)


def flatten(vertices, uvs, parts, item=False):
    out_v, out_uv, tris = [], [], []
    for name, faces in parts.items():
        transform = sheath_transform if item and name == "sheath" else blade_transform if item else None
        for vi, ti in faces:
            new_vi, new_ti = [], []
            for source_vi, source_ti in zip(vi, ti):
                point = vertices[source_vi - 1]
                out_v.append(transform(point) if transform else point)
                out_uv.append(uvs[source_ti - 1])
                new_vi.append(len(out_v))
                new_ti.append(len(out_uv))
            tris.append((new_vi, new_ti))
    return out_v, out_uv, tris


def dimensions(vertices):
    return tuple(max(p[i] for p in vertices) - min(p[i] for p in vertices) for i in range(3))


def main():
    vertices, uvs, parts = load_parts()
    assert all(parts.values()), {name: len(faces) for name, faces in parts.items()}
    world = flatten(vertices, uvs, parts)
    item = flatten(vertices, uvs, parts, item=True)
    tex = Image.open(TEX).convert("RGBA")
    canvas = Image.new("RGB", (1000, 760), (15, 15, 20))
    draw = ImageDraw.Draw(canvas)
    canvas.paste(preview.render(*world, tex, -1.05, -0.18, 0.79, (1000, 370)), (0, 0))
    canvas.paste(preview.render(*item, tex, 0.0, 0.0, 0.0, (1000, 370)), (0, 390))
    counts = " ".join(f"{name}={len(faces)}面" for name, faces in parts.items())
    size = dimensions(world[0])
    draw.text((12, 8), "第三人称：默认坐标原尺寸直接拼接", fill=(235, 245, 255))
    draw.text((12, 374), f"{counts}  整体尺寸={size[0]:.2f}×{size[1]:.2f}×{size[2]:.2f}", fill=(210, 230, 255))
    draw.text((12, 398), "物品栏：默认 item_blade 刀/鞘交叉布局", fill=(235, 245, 255))
    canvas.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
