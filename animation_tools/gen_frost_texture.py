# -*- coding: utf-8 -*-
"""生成名刀·寒霜的混合模型与贴图。

模型：刀身/刀柄/刀镡沿用寒霜原有村正网格，刀鞘改用付丧结月使用的 agito.obj sheath 分组。
贴图：村正半区保留寒霜刀身风格；付丧刀鞘半区保持冰蓝鞘底，重画紫色结绳纹与紫色下绪。
用法: python3 animation_tools/gen_frost_texture.py
"""
import io
import math
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "libs" / "SlashBladeResharped-2.0.5-1.21.1.jar"
OUT_DIR = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost"
MURAMASA_OBJ = "assets/slashblade/model/named/muramasa/muramasa.obj"
MURAMASA_TEX = "assets/slashblade/model/named/muramasa/muramasa.png"
TSUKUMO_OBJ = "assets/slashblade/model/named/agito.obj"
TSUKUMO_TEX = "assets/slashblade/model/named/a_tukumo.png"


def parse_obj(text):
    vertices, uvs, groups = [], [], []
    group = None
    for line in text.splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            vertices.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "vt":
            uvs.append(tuple(map(float, fields[1:3])))
        elif fields[0] == "g":
            group = fields[1]
        elif fields[0] == "f" and group:
            face = []
            for token in fields[1:4]:
                bits = token.split("/")
                face.append((int(bits[0]), int(bits[1])))
            groups.append((group, face))
    return vertices, uvs, groups


def sheath_components(model):
    """拆出付丧鞘的主体、下绪和鞘口环，便于分别着色及避免重叠。"""
    vertices, _, faces = model
    sheath = [(g, f) for g, f in faces if g == "sheath"]
    by_vertex = {}
    for index, (_, face) in enumerate(sheath):
        for vi, _ in face:
            by_vertex.setdefault(vi, []).append(index)
    unseen = set(range(len(sheath)))
    components = []
    while unseen:
        seed = unseen.pop()
        stack, indices = [seed], []
        while stack:
            index = stack.pop()
            indices.append(index)
            for vi, _ in sheath[index][1]:
                for linked in by_vertex[vi]:
                    if linked in unseen:
                        unseen.remove(linked)
                        stack.append(linked)
        points = [vertices[vi - 1] for index in indices for vi, _ in sheath[index][1]]
        lo = tuple(min(point[axis] for point in points) for axis in range(3))
        hi = tuple(max(point[axis] for point in points) for axis in range(3))
        components.append((len(indices), lo, hi, indices))

    # 240 面是完整鞘身；72 面中保留较靠背面的一条下绪；两个重叠鞘口环只留外侧一个。
    chosen = {}
    bodies = [component for component in components if component[0] == 240]
    cords = [component for component in components if component[0] == 72]
    rings = [component for component in components if component[0] == 32]
    if bodies:
        chosen["body"] = bodies[0]
    if cords:
        chosen["cord"] = min(cords, key=lambda component: component[1][2])
    if rings:
        chosen["mouth_ring"] = min(rings, key=lambda component: component[1][0])
    return {name: [sheath[index] for index in component[3]] for name, component in chosen.items()}


def sheath_component_faces(model):
    return [face for faces in sheath_components(model).values() for face in faces]


def sheath_tip_fitting(model):
    """在付丧鞘尾生成贴合鞘身的金色鞘头刀装。"""
    vertices, uvs, _ = model
    body_faces = sheath_components(model).get("body", [])
    tip_faces = []
    for group, face in body_faces:
        points = [vertices[vi - 1] for vi, _ in face]
        if min(point[0] for point in points) < -291.0:
            tip_faces.append((group, face))
    return tip_faces


def frost_tsuba_mesh(gold_uv):
    """生成寒霜专属六向雪花刀镡；返回独立三角面，不再沿用村正花纹。"""
    cx, cy, cz = -36.6, 3.4, 0.0
    x0, x1 = cx - 1.45, cx + 1.45
    triangles = []

    def prism_segment(y0, z0, y1, z1, width):
        dy, dz = y1 - y0, z1 - z0
        length = math.hypot(dy, dz)
        ny, nz = -dz / length * width / 2.0, dy / length * width / 2.0
        points = [(x0, y0 + ny, z0 + nz), (x0, y0 - ny, z0 - nz),
                  (x0, y1 + ny, z1 + nz), (x0, y1 - ny, z1 - nz),
                  (x1, y0 + ny, z0 + nz), (x1, y0 - ny, z0 - nz),
                  (x1, y1 + ny, z1 + nz), (x1, y1 - ny, z1 - nz)]
        indices = ((0, 2, 3), (0, 3, 1), (4, 5, 7), (4, 7, 6),
                   (0, 1, 5), (0, 5, 4), (2, 6, 7), (2, 7, 3),
                   (1, 3, 7), (1, 7, 5), (0, 4, 6), (0, 6, 2))
        triangles.extend(tuple((points[index], gold_uv) for index in face) for face in indices)

    # 六根主枝构成雪花骨架；每根主枝末段再分出两根短枝，形成清楚的雪花纹路。
    for arm in range(6):
        angle = math.radians(arm * 60.0)
        uy, uz = math.cos(angle), math.sin(angle)
        prism_segment(cy + uy * 1.2, cz + uz * 1.2,
                      cy + uy * 10.8, cz + uz * 10.8, 1.85)
        branch_y, branch_z = cy + uy * 7.0, cz + uz * 7.0
        for turn in (-42.0, 42.0):
            branch_angle = angle + math.radians(turn)
            by, bz = math.cos(branch_angle), math.sin(branch_angle)
            prism_segment(branch_y, branch_z,
                          branch_y + by * 4.2, branch_z + bz * 4.2, 1.35)
    return triangles


def reserve_gold_swatch(image):
    """在村正半图的透明处放一枚专供雪花刀镡采样的金色材质点。"""
    px = image.load()
    for y in range(2, image.height - 2):
        for x in range(2, image.width - 2):
            if all(px[xx, yy][3] < 16 for yy in range(y - 1, y + 2) for xx in range(x - 1, x + 2)):
                for yy in range(y - 1, y + 2):
                    for xx in range(x - 1, x + 2):
                        px[xx, yy] = (236, 188, 56, 255)
                return ((x + 0.5) / image.width, 1.0 - (y + 0.5) / image.height)
    raise RuntimeError("寒霜贴图中没有可放置金色刀镡材质点的透明区域")


def write_mixed_obj(mur, tsu, path, gold_uv):
    """写出寒霜世界模型，以及由寒霜自身部件重排得到的物品栏模型。"""
    mvs, muv, mfaces = mur
    tvs, tuv, _ = tsu
    sheath_faces = sheath_component_faces(tsu)

    def is_old_tsuba(group, face):
        """识别并剔除村正原刀镡，避免金色雪花外仍套着村正花纹。"""
        if group not in ("blade", "blade_damaged"):
            return False
        points = [mvs[vi - 1] for vi, _ in face]
        cx = sum(point[0] for point in points) / len(points)
        return -45.0 <= cx < -15.0 and max(abs(point[2]) for point in points) >= 4.0

    def blade_body(group, face):
        """物品栏拼装用的刀条：去掉 blade 内重复的柄形网格和旧村正刀镡。"""
        if group not in ("blade", "blade_damaged"):
            return True
        cx = sum(mvs[vi - 1][0] for vi, _ in face) / len(face)
        return cx < -15.0 and not is_old_tsuba(group, face)

    def world_body(group, face):
        """实战模型必须保留初版 blade 分组自带的柄形网格，只替换旧刀镡。"""
        return group not in ("blade", "blade_damaged") or not is_old_tsuba(group, face)

    # SlashBlade 实战渲染按单个命名分组取模型，不会保证把 handle 与 blade 自动拼接。
    # 初版村正 blade 本身就包含一套完整刀柄，因此世界模型必须原样保留这部分；
    # 独立 handle 分组仍留给拔刀/组装等专用渲染使用。
    selected = [("muramasa", g, f, g, None) for g, f in mfaces
                if g in ("handle", "blade", "blade_damaged", "blade_fragment")
                and world_body(g, f)]
    selected += [("tsukumo", g, f, "sheath", None) for g, f in sheath_faces]

    # 村正的物品栏不是把整刀一起旋转，而是分别重排刀与鞘，形成分离的十字交叉。
    # 以下两组仿射参数分别由村正世界模型的刀条、鞘身与 item_blade 同源顶点反算；
    # 寒霜保留自己的刀身、刀柄、雪花刀镡和冰蓝刀鞘，只完全复用村正的摆放方式。
    def item_blade_transform(point):
        x, y, z = point
        return (-0.3513422091 * x - 1.2292924936 * y + 0.0000086237 * z - 63.2631436785,
                0.4696877503 * x - 0.8699876138 * y - 0.0000101678 * z + 57.8811116816,
                1.4039793118 * z + 15.5930000000)

    def item_sheath_transform(point):
        x, y, z = point
        return (0.3425939586 * x + 0.7708359100 * y - 0.0000083467 * z + 63.8568170358,
                0.4477656188 * x - 0.6248078361 * y - 0.0000066559 * z + 62.4343619704,
                -0.6393679184 * z + 8.4590103634)

    item_blade = [("muramasa", g, f) for g, f in mfaces
                  if g in ("handle", "blade") and blade_body(g, f)]
    item_damaged = [("muramasa", g, f) for g, f in mfaces
                    if g in ("handle", "blade_damaged") and blade_body(g, f)]
    item_sheath = [("tsukumo", g, f) for g, f in sheath_faces]
    for source, group, face in item_blade:
        selected.append((source, group, face, "item_blade", item_blade_transform))
    for source, group, face in item_sheath:
        selected.append((source, group, face, "item_blade", item_sheath_transform))
    for source, group, face in item_damaged:
        selected.append((source, group, face, "item_damaged", item_blade_transform))
    for source, group, face in item_sheath:
        selected.append((source, group, face, "item_damaged", item_sheath_transform))
    for source, group, face in item_blade:
        selected.append((source, group, face, "item_bladens", item_blade_transform))
    # 保留旧版/村正提供的物品栏背板分组；当前渲染器不强制绘制它，但其他兼容渲染路径会读取。
    selected += [("muramasa", g, f, "item_back", None) for g, f in mfaces if g == "item_back"]

    out = ["# Frost Blade: own blade + simplified Tsukumo sheath", "o frost"]
    vertices, texcoords, faces = [], [], []
    for source, _source_group, face, output_group, transform in selected:
        src_v, src_uv = (mvs, muv) if source == "muramasa" else (tvs, tuv)
        remapped = []
        for vi, ti in face:
            point = src_v[vi - 1]
            vertices.append(transform(point) if transform else point)
            u, v = src_uv[ti - 1]
            u = u * 0.5 + (0.5 if source == "tsukumo" else 0.0)
            texcoords.append((u, v))
            remapped.append(len(vertices))
        faces.append((output_group, remapped))

    def append_snowflake(group, transform=None):
        for triangle in frost_tsuba_mesh(gold_uv):
            remapped = []
            for point, (u, v) in triangle:
                vertices.append(transform(point) if transform else point)
                texcoords.append((u * 0.5, v))
                remapped.append(len(vertices))
            faces.append((group, remapped))

    # 世界、损坏状态、无鞘状态和物品栏都使用同一枚寒霜雪花刀镡。
    append_snowflake("blade")
    append_snowflake("blade_damaged")
    append_snowflake("item_blade", item_blade_transform)
    append_snowflake("item_damaged", item_blade_transform)
    append_snowflake("item_bladens", item_blade_transform)

    # 每个命名分组只输出一次。部分 OBJ 加载器遇到重复的同名 g 段时会覆盖前段，
    # 这正会导致最后追加的雪花刀镡把完整刀身替掉。
    group_order = {name: index for index, name in enumerate(
        ("handle", "blade", "blade_damaged", "blade_fragment", "sheath",
         "item_blade", "item_damaged", "item_bladens", "item_back"))}
    faces.sort(key=lambda item: group_order[item[0]])

    out += ["v %.6f %.6f %.6f" % point for point in vertices]
    out += ["vt %.6f %.6f" % uv for uv in texcoords]
    current = None
    for group, face in faces:
        if group != current:
            out.append("g " + group)
            current = group
        out.append("f " + " ".join(f"{index}/{index}" for index in face))
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def muramasa_part(group, vertices, face):
    if group == "effect":
        return "other"
    if group == "handle":
        return "handle"
    if group == "blade":
        points = [vertices[i - 1] for i, _ in face]
        cx = sum(p[0] for p in points) / 3.0
        zmax = max(abs(p[2]) for p in points)
        if cx >= -15.0:
            return "handle"
        if cx >= -45.0 and zmax >= 4.0:
            return "tsuba"
        return "blade"
    return "other"


def recolor_muramasa(base, model):
    vertices, uvs, faces = model
    w, h = base.size
    priorities = {"other": 10, "blade": 30, "tsuba": 50, "handle": 80}
    labels = Image.new("L", base.size, 0)
    draw = ImageDraw.Draw(labels)
    for group, face in sorted(faces, key=lambda item: priorities[muramasa_part(item[0], vertices, item[1])]):
        part = muramasa_part(group, vertices, face)
        points = [(uvs[ti - 1][0] * w, (1.0 - uvs[ti - 1][1]) * h) for _, ti in face]
        draw.polygon(points, fill=priorities[part])
    # OBJ 的三角形 UV 边缘会落在半像素处；把分类遮罩向外扩一像素，避免刀柄
    # 缠绳边缘残留村正原本的棕色细线，看起来像缺了一块或套错模型。
    labels = labels.filter(ImageFilter.MaxFilter(3))
    reverse = {v: k for k, v in priorities.items()}
    src, mask = base.load(), labels.load()
    out = Image.new("RGBA", base.size)
    dst = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a < 128:
                dst[x, y] = (r, g, b, a)
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            part = reverse.get(mask[x, y], "other")
            if part == "handle":
                # 修正旧版刀柄的一处棕/金错色：实际柄面统一成深蓝缠柄。
                dst[x, y] = (int(18 + lum * 42), int(25 + lum * 58), int(70 + lum * 105), a)
            elif part == "tsuba":
                # 寒霜刀镡统一改为金色，同时保留原贴图明暗，避免看成一块扁平黄片。
                dst[x, y] = (int(126 + lum * 124), int(78 + lum * 136), int(12 + lum * 60), a)
            elif part == "blade":
                dst[x, y] = (int(30 + lum * 210), int(38 + lum * 215), int(55 + lum * 220), a)
            else:
                dst[x, y] = (r, g, b, a)
    return out


def tsukumo_sheath_masks(model, size):
    vertices, uvs, _ = model
    w, h = size
    masks = {name: Image.new("L", size, 0) for name in ("body", "cord", "mouth_ring", "tip_fitting")}
    components = sheath_components(model)
    # 贴图遮罩和最终 OBJ 使用完全相同的精简网格，被删掉的付丧装饰不会残留。
    for name in ("body", "cord", "mouth_ring"):
        draw = ImageDraw.Draw(masks[name])
        for group, face in components.get(name, []):
            points = [(uvs[ti - 1][0] * w, (1.0 - uvs[ti - 1][1]) * h) for _, ti in face]
            draw.polygon(points, fill=255)
    tip_draw = ImageDraw.Draw(masks["tip_fitting"])
    for group, face in sheath_tip_fitting(model):
        points = [(uvs[ti - 1][0] * w, (1.0 - uvs[ti - 1][1]) * h) for _, ti in face]
        tip_draw.polygon(points, fill=255)
    return masks


def paint_tsukumo_sheath(base, model):
    """统一冰蓝鞘底、纯紫下绪，并绘制村正徽纹与阎魔刀编绳式纹路。"""
    base = base.resize((256, 512), Image.Resampling.NEAREST)
    masks = tsukumo_sheath_masks(model, base.size)
    bm, cm = masks["body"].load(), masks["cord"].load()
    rm, tm = masks["mouth_ring"].load(), masks["tip_fitting"].load()
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    dst = out.load()
    # 鞘底统一冰蓝，下绪统一紫色；鞘口与鞘头按村正样式使用金色刀装。
    for y in range(out.height):
        for x in range(out.width):
            if rm[x, y] or tm[x, y]:
                dst[x, y] = (222, 174, 48, 255)
            elif cm[x, y]:
                dst[x, y] = (112, 55, 170, 255)
            elif bm[x, y]:
                dst[x, y] = (126, 181, 216, 255)

    mask = masks["body"].load()
    purple_dark = (72, 30, 125, 255)
    purple_mid = (119, 54, 181, 255)
    purple_light = (185, 105, 232, 255)

    def masked_line(points, color, width=2):
        layer = Image.new("RGBA", out.size)
        ImageDraw.Draw(layer).line(points, fill=color, width=width, joint="curve")
        lp = layer.load()
        for yy in range(out.height):
            for xx in range(out.width):
                if lp[xx, yy][3] and mask[xx, yy]:
                    out.putpixel((xx, yy), lp[xx, yy])

    # 付丧鞘的两块大侧面（z+ / z-）共用 x=24..46 的 UV 岛，因此只在这条
    # 侧面岛上绘制；窄边 x=20..24、46..50 保持纯冰蓝，不再把雪花画到鞘脊上。
    masked_line([(35, 26), (35, 482)], purple_dark, 1)
    for cx, cy in ((35, 58), (35, 126), (35, 194), (35, 262), (35, 330), (35, 398), (35, 466)):
        radius = 9
        for angle in (0, 60, 120):
            rad = math.radians(angle)
            dx, dy = math.cos(rad), math.sin(rad)
            x0, y0 = cx - dx * radius, cy - dy * radius
            x1, y1 = cx + dx * radius, cy + dy * radius
            masked_line([(x0, y0), (x1, y1)], purple_mid, 2)
            for sign in (-1, 1):
                bx, by = cx + dx * radius * .58 * sign, cy + dy * radius * .58 * sign
                direction = angle + (180 if sign < 0 else 0)
                for branch in (-38, 38):
                    br = math.radians(direction + branch)
                    masked_line([(bx, by), (bx - math.cos(br) * 3.5, by - math.sin(br) * 3.5)],
                                purple_light, 1)
        # 村正徽纹式菱核，细节控制在侧面宽度内。
        masked_line([(cx, cy - 3), (cx + 3, cy), (cx, cy + 3), (cx - 3, cy), (cx, cy - 3)],
                    purple_light, 1)
    return out


def paint_snowflake(img):
    """保留此前已确认的黑底雪花徽记，并清掉旧绿色火焰精灵。"""
    px = img.load()
    green = []
    for y in range(120, 200):
        for x in range(150, 220):
            r, g, b, a = px[x, y]
            if a > 100 and g > r + 10 and g > b + 10:
                green.append((x, y))
    if green:
        x0, y0 = min(x for x, _ in green), min(y for _, y in green)
        x1, y1 = max(x for x, _ in green), max(y for _, y in green)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[x, y] = (0, 0, 0, 0)
        pattern = ("....#....", "....#....", "...###...", "#...#...#", ".#..#..#.", "..#.#.#..",
                   "...###...", "###.#.###", "###.#.###", "...###...", "..#.#.#..", ".#..#..#.",
                   "#...#...#", "...###...", "....#....", "....#....")
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if pattern[(y - y0) * 16 // (y1 - y0 + 1)][(x - x0) * 9 // (x1 - x0 + 1)] == "#":
                    px[x, y] = (255, 255, 255, 255)
    # 不覆盖村正 item_blade 网格所用的上方刀框区域；此前在这里另画大雪花，
    # 会被多个立体面重复采样，最终正是物品栏里那块错误的大色块。


def main():
    with zipfile.ZipFile(JAR) as archive:
        mur_text = archive.read(MURAMASA_OBJ).decode("utf-8", errors="replace")
        tsu_text = archive.read(TSUKUMO_OBJ).decode("utf-8", errors="replace")
        mur_tex = Image.open(io.BytesIO(archive.read(MURAMASA_TEX))).convert("RGBA")
        tsu_tex = Image.open(io.BytesIO(archive.read(TSUKUMO_TEX))).convert("RGBA")
    mur, tsu = parse_obj(mur_text), parse_obj(tsu_text)
    left = recolor_muramasa(mur_tex, mur)
    paint_snowflake(left)
    gold_uv = reserve_gold_swatch(left)
    right = paint_tsukumo_sheath(tsu_tex, tsu)
    atlas = Image.new("RGBA", (512, 512))
    atlas.paste(left, (0, 0), left)
    atlas.paste(right, (256, 0), right)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    atlas.save(OUT_DIR / "frost.png")
    write_mixed_obj(mur, tsu, OUT_DIR / "frost.obj", gold_uv)
    print("输出:", OUT_DIR / "frost.png", OUT_DIR / "frost.obj")


if __name__ == "__main__":
    main()
