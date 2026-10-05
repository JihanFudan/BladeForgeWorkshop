# -*- coding: utf-8 -*-
"""
build_parts_model.py —— 从拔刀剑本体 jar 的 blade.obj 切出「刀条/刀镡/刀柄/刀鞘」
四部件的真实网格，导出成一套同源数据：

  1. 游戏内动画用的部件网格 JSON
     src/main/resources/assets/slashbladeresh_slashblad/assembly/blade_parts.json
  2. 部件贴图（拔刀剑本体的木偶 wood.png / 竹光 bamboo.png 材质表复用：
     横向平铺 TILE_U 份并补全透明区，配合 u/TILE_U 的 UV，
     在 CLAMP 采样下与原版 REPEAT 逐像素一致，杜绝拉丝）
     src/main/resources/assets/slashbladeresh_slashblad/textures/assembly/sb_wood.png
     src/main/resources/assets/slashbladeresh_slashblad/textures/assembly/sb_bamboo.png
  3. Blender / MMD 可加载的预览模型（四部件挂四根骨骼）
     blender_preview/blade_assembly_parts.pmx
  4. Blender 免插件可直接导入的"模型+组装动画"一体文件（含四部件网格与
     assembly_vmd.py 编排的关键帧），改完动画导出同格式 glb 再跑
     export_assembly_vmd.py 即可写回游戏
     blender_preview/blade_assembly.glb

因为四个部件来自同一个 blade.obj，落位后必然严丝合缝（对齐问题的根治）。
刀条额外加了一截"茎"（nakago）薄盒：刀镡滑过来时刀条右侧穿过刀镡、
微微露出，之后被刀柄罩住——与真实日本刀的装配关系一致。

除组装动画的四个部件外，本脚本还导出多件**手持部件**（hold_*，同一份 JSON）：
物品拿在手里时画的就是它们——金属刀条用大太刀的刀刃、木刀条用木偶的刀刃、
竹刀条用竹光的刀刃、刀鞘用**木偶的刀鞘**（blade.obj 的 sheath 分组 +
wood.png 材质表），全部取自拔刀剑本体网格与材质表。
手持部件存的是"物品模型坐标"（0..1 立方、中心 0.5、刀尖朝右上斜放），
与原版 display 变换的旋转轴约定一致。

坐标归一化（与 assembly_vmd.py / AssemblyStageModel.java 同一约定）：
  骨原点 = 整刀的刀镡中心；X 沿刀身、刀尖朝 -X；Y 屏幕上为正；Z 朝摄像机；
  单位 = 格（整刀连鞘约 1.18 格长）。

用法: python3 animation_tools/build_parts_model.py
"""
import io
import json
import math
import os
import struct
import sys
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

JAR = os.path.join(ROOT, "libs", "SlashBladeResharped-2.0.5-1.21.1.jar")
OBJ = "assets/slashblade/model/blade.obj"
TEX_WOOD = "assets/slashblade/model/wood.png"
TEX_BAMBOO = "assets/slashblade/model/bamboo.png"

# ---- 手持 3D 模型（拿在手中的外观）------------------------------------
# 拔刀剑本版本（重锋 2.0.5）里没有单独叫"大太刀"的刀，同一套刀身网格靠材质表
# 区分刀型；这里取"大太刀"那套网格 = 魔剑「阎魔刀」（刀鞘是加饰长鞘，刀身是钢刃）。
# 想换成枯石大刀的网格，把 ODACHI_OBJ / TEX_ODACHI 改成
#   assets/slashblade/model/named/dios/dios.obj / assets/slashblade/model/named/dios/koseki.png
# 重跑本脚本即可（两份网格的 blade / sheath 分组同名，坐标同一套原点）。
ODACHI_OBJ = "assets/slashblade/model/named/yamato.obj"
TEX_ODACHI = "assets/slashblade/model/named/yamato.png"
# 舞台坐标里刀身沿 X（刀尖朝 -X）；绕 Z 转到与原版剑图标一致的斜向（刀尖朝右上）。
# 物品空间里刀尖的方位角 = 180° + rot，所以 -135° 对应"刀尖朝右上 45°"（原版剑图标那样）。
HOLD_ROT = -135.0
# 刀尖再往前压一点：方位角从 45° 抬到 102°，第一人称里刀尖就从"戳向自己"变成"向前倾"。
# 屏幕上的斜度只由 display 的 rz 决定，所以第三人称那边同步把 rz 减掉同样的量，
# 两边的观感才不会互相打架（见 models/item/*.json 的 display 段）。
HOLD_BLADE_ROT = -78.0
# 刀鞘要调转上下：鞘口朝上、鞘尾朝下，所以比刀条多转 180°
HOLD_SHEATH_ROT = HOLD_BLADE_ROT + 180.0
HOLD_PART_CN = {
    "hold_blade_metal": "金属刀条（大太刀刀刃）",
    "hold_blade_wood": "木刀条（木偶刀刃）",
    "hold_blade_bamboo": "竹刀条（竹光刀刃）",
    "hold_sheath": "刀鞘（木偶刀鞘）",
    "hold_blade_grip": "刀条（被钳子夹着）",
    "hold_tongs_blade": "钳子（夹刀条）",
    "hold_ingot": "灼热锭子",
    "hold_ingot_flat": "灼热锭子（台面平放，宽面朝上）",
    "hold_ingot_grip": "灼热锭子（被钳子夹着）",
    "hold_tongs_ingot": "钳子（夹锭子）",
}

# ---- 钳子 / 锭子（"背包里有钳子时夹着烧红工件"的模型）------------------------
# 钳子材质表 sb_iron.png：**左半冷灰铁、右半热渍暖铁**，纵向都是"上亮下暗"三段。
# 盒子的顶面取亮带、侧面取中间带、底面取暗带就有立体感（AssemblyStageModel
# 给所有顶点都写同一个法线，不吃方向光照，明暗只能靠贴图本身带）。
# 钳口贴着烧红工件会烤出火色——钳口三个面采右半的暖区，钳臂/钳柄采左半冷区。
IRON_SHEET = "sb_iron"
IRON_TOP_UV = (0.05, 0.06, 0.45, 0.26)
IRON_SIDE_UV = (0.05, 0.36, 0.45, 0.62)
IRON_BOTTOM_UV = (0.05, 0.72, 0.45, 0.94)
IRON_HOT_TOP_UV = (0.55, 0.06, 0.95, 0.26)
IRON_HOT_SIDE_UV = (0.55, 0.36, 0.95, 0.62)
IRON_HOT_BOTTOM_UV = (0.55, 0.72, 0.95, 0.94)
# 锭子材质表：sb_hot.png 是烧红的钢，配合每件物品的乘色区分铁/钢/融合钢
HOT_SHEET = "sb_hot"
HOT_TOP_UV = (0.25, 0.06, 0.75, 0.30)
HOT_SIDE_UV = (0.25, 0.40, 0.75, 0.64)
HOT_BOTTOM_UV = (0.25, 0.74, 0.75, 0.94)

NS = "slashbladeresh_slashblad"
RES = os.path.join(ROOT, "src", "main", "resources", "assets", NS)
OUT_JSON = os.path.join(RES, "assembly", "blade_parts.json")
OUT_TEX_DIR = os.path.join(RES, "textures", "assembly")
OUT_PMX = os.path.join(ROOT, "blender_preview", "blade_assembly_parts.pmx")
OUT_GLB = os.path.join(ROOT, "blender_preview", "blade_assembly.glb")

# 归一化：模型单位 -> 格；原点放在刀镡中心附近
ORIGIN_X, ORIGIN_Y = -30.0, 3.0
SCALE = 1.18 / 345.0          # 整刀(连鞘尾到柄头)约 1.18 格

# 原版 blade.obj 有少量面 u 最大到 4.51（刀鼻环带沿材质表横向平铺采样）。
# Minecraft 的贴图采样是 CLAMP_TO_EDGE，折叠取模会让这些面被整张表"拉丝"。
# 解法：把材质表横向平铺 TILE_U 份、所有 u 除以 TILE_U —— 采样结果与原版
# REPEAT 逐像素一致，且全部落在 [0,1] 内，任何查看器都不再拉伸。
TILE_U = 5

PART_CN = {"blade_blank": "刀条", "tsuba": "刀镡", "handle": "刀柄", "sheath": "刀鞘"}
PART_ORDER = ["blade_blank", "tsuba", "handle", "sheath"]


# --------------------------------------------------------------------------
# OBJ 解析与部件切分
# --------------------------------------------------------------------------

def load_obj(path=OBJ):
    data = zipfile.ZipFile(JAR).read(path).decode("utf-8", errors="replace")
    vs, vts = [], []
    group = None
    faces = {}
    for line in data.splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            vs.append(tuple(map(float, t[1:4])))
        elif t[0] == "vt":
            vts.append(tuple(map(float, t[1:3])))
        elif t[0] == "g":
            group = t[1]
            faces.setdefault(group, [])
        elif t[0] == "f" and group:
            vi = [int(x.split("/")[0]) for x in t[1:4]]
            ti = [int(x.split("/")[1]) for x in t[1:4]]
            faces[group].append((vi, ti))
    return vs, vts, faces


def norm(mx, my, mz):
    """拔刀剑模型坐标 -> 舞台坐标（格，X右 Y上 Z朝摄像机，原点在刀镡中心）。"""
    return ((mx - ORIGIN_X) * SCALE, -(my - ORIGIN_Y) * SCALE, mz * SCALE)


def tang_box(uv_rect, x1=14.0):
    """刀条右侧多出的"茎"：薄盒 12 三角，UV 取刀条根部材质带的平均色块。

    x1 控制刀尾伸到多长：直接手持用默认值（留一截看得见的刀尾），
    被钳子夹着那份要截短到钳口尖端，不然刀尾会从钳臂中间穿出去（穿模）。"""
    x0, y0, y1, z0, z1 = -42.0, 0.4, 5.6, -1.15, 1.15
    p = [norm(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    # p 索引: (x0y0z0)=0 (x0y0z1)=1 (x0y1z0)=2 (x0y1z1)=3 (x1y0z0)=4 (x1y0z1)=5 (x1y1z0)=6 (x1y1z1)=7
    tris = [(0, 2, 3), (0, 3, 1), (4, 5, 7), (4, 7, 6), (0, 1, 5), (0, 5, 4),
            (2, 6, 7), (2, 7, 3), (1, 3, 7), (1, 7, 5), (0, 4, 6), (0, 6, 2)]
    u0, v0, u1, v1 = uv_rect
    uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
    out = []
    for k, tri in enumerate(tris):
        uv = uvs[k % 4]
        for vi in tri:
            out.append((p[vi], uv))
    return out


def _machi_uv_rect(vs, vts, faces):
    """给刀尾“茎”取刀身本色。

    旧实现把镡前各面的 UV 全部求平均，平均点恰好落进材质表顶部的金黄纹样，
    因而钢、粗制、覆土、烧红等金属刀条都会长出一截黄色刀尾。刀身主体在
    原始 64×128 材质表的 (x=16, y=64) 一带；横向平铺后仍取第一格的这块
    2×4 像素色带。这样冷刀尾是对应钢色，烧红材质表又会自然得到相同热色，
    木／竹材质表也仍会取到各自本色，而不是统一染成黄色。
    """
    return (15.0 / (64.0 * TILE_U), 62.0 / 128.0,
            17.0 / (64.0 * TILE_U), 66.0 / 128.0)


# 薄盒 12 三角的顶点索引表（每个面两个三角）
_BOX_TRIS = [(0, 2, 3), (0, 3, 1), (4, 5, 7), (4, 7, 6), (0, 1, 5), (0, 5, 4),
             (2, 6, 7), (2, 7, 3), (1, 3, 7), (1, 7, 5), (0, 4, 6), (0, 6, 2)]


def _stage_box(x0, x1, y0, y1, z0, z1, top_uv, side_uv, bottom_uv):
    """舞台坐标（格）里造一个薄盒；顶/侧/底各取材质表上不同的明暗带。"""
    p = [(x0, y0, z0), (x0, y0, z1), (x0, y1, z0), (x0, y1, z1),
         (x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (x1, y1, z1)]
    # 面顺序与 _BOX_TRIS 对应：x0、x1、y0(底)、y1(顶)、z1、z0
    face_uv = [side_uv, side_uv, bottom_uv, top_uv, side_uv, side_uv]
    out = []
    for k, tri in enumerate(_BOX_TRIS):
        rect = face_uv[k // 2]
        u0, v0, u1, v1 = rect
        corners = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        uv = corners[k % 4]
        for vi in tri:
            out.append(((p[vi][0], p[vi][1], p[vi][2]), uv))
    return out


def _tongs(gap, jaw_len, z_jaw=0.010, axis="y"):
    """一把火钳：两片钳口咬住工件，往后收成铆钉，再接两截钳柄。

    gap 是钳口内侧面到中心面的距离——必须贴着工件表面（比工件半高大一丝），
    钳子才像真夹着东西，而不是悬在工件上下；
    jaw_len 是钳口长度，要**盖过工件尾端**：钳臂从钳口尖端起步，工件要是
    伸得比钳口还长，就会从两根钳臂中间穿出去（上一版刀尾穿模的真凶）。
    z_jaw 是钳口沿钳身横向的半宽，不能宽过工件，不然钳子看着像一块灰板。
    axis 是钳口张开方向："y" 从上下咬（夹锭子，钳口咬住锭子上下宽面）；
    "z" 沿刀面法线咬（夹刀条：钳口贴住刀茎的两个大面，正前视里钳子顺着
    刀身一条线延伸——铁匠夹刀条就是这个姿势，上一版从上下咬才显得悬空）。
    钳口采热渍暖铁色带（被工件烤出火色），钳臂/钳柄采冷灰铁。"""
    jaw_t = 0.013
    z_arm = max(0.009, z_jaw - 0.002)
    juv = (IRON_HOT_TOP_UV, IRON_HOT_SIDE_UV, IRON_HOT_BOTTOM_UV)
    tris = []
    # 钳口（两片）：内侧面贴工件；尖端一段收窄一点，钳口就有"咬合"的层次
    for sy in (1.0, -1.0):
        tris += _stage_box(0.006, jaw_len - 0.020, sy * gap, sy * (gap + jaw_t),
                           -z_jaw, z_jaw, *juv)
        tris += _stage_box(jaw_len - 0.020, jaw_len, sy * gap, sy * (gap + jaw_t),
                           -z_jaw * 0.7, z_jaw * 0.7, *juv)
    # 钳臂：从钳口尖端一路直杆到铆钉（高度和钳口一致，不断档）；
    # 工件在钳口尖端之前就结束了，钳臂不会再被穿出去。
    pivot_x = jaw_len + 0.150
    for sy in (1.0, -1.0):
        tris += _stage_box(jaw_len, pivot_x, sy * gap, sy * (gap + jaw_t),
                           -z_arm, z_arm, IRON_TOP_UV, IRON_SIDE_UV, IRON_BOTTOM_UV)
    # 铆钉：横跨两片钳臂的交汇块（把上下钳臂真正连在一起，不留悬空缝）
    tris += _stage_box(pivot_x, pivot_x + 0.040, -(gap + jaw_t + 0.004), gap + jaw_t + 0.004,
                       -z_arm, z_arm, IRON_TOP_UV, IRON_SIDE_UV, IRON_BOTTOM_UV)
    # 钳柄：过了铆钉与钳臂几乎成一条直线，尾段微微外翻成握把
    # （火钳的经典轮廓 = 两根长铁杆在铆钉处交叉；上一版钳柄突然向中心收，
    #  剪影断成几截，看着像坏掉的骨架）
    handle_x = pivot_x + 0.185
    h0 = pivot_x + 0.040
    h1 = (h0 + handle_x) / 2.0
    for sy in (1.0, -1.0):
        tris += _stage_box(h0, h1, sy * (gap - 0.002), sy * (gap + jaw_t + 0.002),
                           -0.010, 0.010, IRON_TOP_UV, IRON_SIDE_UV, IRON_BOTTOM_UV)
        tris += _stage_box(h1, handle_x, sy * (gap + 0.004), sy * (gap + jaw_t + 0.008),
                           -0.010, 0.010, IRON_TOP_UV, IRON_SIDE_UV, IRON_BOTTOM_UV)
    if axis == "z":
        # 绕 X 转 90°：原本沿 y 的张开/收拢改成沿 z（钳口咬刀条的两个宽面）
        tris = [((x, -z, y), uv) for ((x, y, z), uv) in tris]
    return tris


def _ingot(stage_x=0.045):
    """钳口里那块烧红的锭子：上窄下宽的短条，上下对称（半高 0.025），
    钳口按这个半高合拢，正好咬在锭子上下表面。"""
    tris = []
    tris += _stage_box(stage_x - 0.085, stage_x + 0.115, -0.025, 0.007, -0.023, 0.023,
                       HOT_TOP_UV, HOT_SIDE_UV, HOT_BOTTOM_UV)
    tris += _stage_box(stage_x - 0.065, stage_x + 0.095, 0.007, 0.025, -0.018, 0.018,
                       HOT_TOP_UV, HOT_SIDE_UV, HOT_BOTTOM_UV)
    return tris


def _ingot_flat(scale=2.4):
    """工作台面（FIXED 视角）平放用的大锭子。

    手持那份 hold_ingot 的网格把"宽面法线"烤成了近似 +X，走 FIXED 变换链后
    锭子是侧着立在台面上的。这里把舞台坐标的锭子绕 X 转 90°（法线 Y→Z），
    再整体放大——经 FIXED display + FloatingItem 的 Rx90 之后正好宽面朝上
    平放，顶面的亮带也朝上，光照观感才对。"""
    tris = []
    for (p, uv) in _ingot():
        x, y, z = p
        tris.append(((x * scale, -z * scale, y * scale), uv))
    return _ItemFrame(tris, 0.0).apply(tris)


def classify_blade_face(cx, zmax):
    """blade 分组里一个三角形属于哪个部件（按面中心 X / 厚度判定）。"""
    if cx >= -15.0:
        return "handle"
    if cx >= -45.0 and zmax >= 4.0:
        return "tsuba"
    return "blade_blank"


def split_parts():
    vs, vts, faces = load_obj()
    parts = {name: [] for name in PART_ORDER}
    machi_uvs = []
    for vi, ti in faces["blade"]:
        pts = [vs[i - 1] for i in vi]
        cx = sum(p[0] for p in pts) / 3.0
        zmax = max(abs(p[2]) for p in pts)
        uvs = [vts[i - 1] for i in ti]
        part = classify_blade_face(cx, zmax)
        if part == "blade_blank" and -62.0 <= cx <= -42.0:
            machi_uvs.extend(uvs)
        for k in range(3):
            # u 除以 TILE_U：配合横向平铺后的材质表，等价于原版 REPEAT 采样，
            # 且不再需要 %1.0 折叠（折叠会在刀鼻环带上造成整表拉丝）
            parts[part].append((norm(*pts[k]), (uvs[k][0] / TILE_U, 1.0 - uvs[k][1])))
    for vi, ti in faces["sheath"]:
        pts = [vs[i - 1] for i in vi]
        uvs = [vts[i - 1] for i in ti]
        for k in range(3):
            parts["sheath"].append((norm(*pts[k]), (uvs[k][0] / TILE_U, 1.0 - uvs[k][1])))

    if machi_uvs:
        us = [u for u, _ in machi_uvs]
        vvs = [v for _, v in machi_uvs]
        cu, cv = sum(us) / len(us), sum(vvs) / len(vvs)
        rect = ((cu - 0.004) / TILE_U, 1.0 - (cv + 0.012),
                (cu + 0.004) / TILE_U, 1.0 - (cv - 0.004))
    else:
        rect = (0.5, 0.5, 0.51, 0.51)
    parts["blade_blank"].extend(tang_box(rect))
    return parts


# --------------------------------------------------------------------------
# 导出 1：游戏内 JSON（每部件一个浮点数组：[x,y,z,u,v] * 3 顶点/三角）
# --------------------------------------------------------------------------

# --------------------------------------------------------------------------
# 手持部件：物品"拿在手中"时画的 3D 网格（拔刀剑原版刀刃/刀鞘分组）
# --------------------------------------------------------------------------

def _collect(obj_path, group, only_blade_blank):
    """取某个拔刀剑模型分组的三角（可选只保留"刀条"部分），带 TILE_U 归一化 UV。

    返回 (三角表, 根部 UV 块)——后者给"茎"（刀尾）用。"""
    vs, vts, faces = load_obj(obj_path)
    out = []
    for vi, ti in faces[group]:
        pts = [vs[i - 1] for i in vi]
        if only_blade_blank:
            cx = sum(p[0] for p in pts) / 3.0
            zmax = max(abs(p[2]) for p in pts)
            if classify_blade_face(cx, zmax) != "blade_blank":
                continue
        uvs = [vts[i - 1] for i in ti]
        for k in range(3):
            out.append((norm(*pts[k]), (uvs[k][0] / TILE_U, 1.0 - uvs[k][1])))
    return out, _machi_uv_rect(vs, vts, faces)


class _ItemFrame:
    """把舞台坐标换算进"物品模型坐标"（0..1 立方、中心 0.5）。

    先绕 Z 转到手持该有的斜向，再把**锚点部件**的包围盒中心摆到立方体中心——
    这样同一套手持变换（原版 display，以方块中心为轴）就能直接把部件端到手上。
    钳子、锭子这类"附属件"用主工件的锚点，才能保证夹在正确的位置上。"""

    def __init__(self, anchor_tris, rot_deg):
        a = math.radians(rot_deg)
        self.ca, self.sa = math.cos(a), math.sin(a)
        rot = [self._rotate(p) for (p, _) in anchor_tris]
        self.ox = 0.5 - (min(p[0] for p in rot) + max(p[0] for p in rot)) / 2.0
        self.oy = 0.5 - (min(p[1] for p in rot) + max(p[1] for p in rot)) / 2.0
        self.oz = 0.5 - (min(p[2] for p in rot) + max(p[2] for p in rot)) / 2.0

    def _rotate(self, p):
        x, y, z = p
        return (x * self.ca - y * self.sa, x * self.sa + y * self.ca, z)

    def apply(self, tris):
        out = []
        for (p, uv) in tris:
            x, y, z = self._rotate(p)
            out.append(((x + self.ox, y + self.oy, z + self.oz), uv))
        return out


def _to_item_space(tris, rot_deg=HOLD_ROT):
    return _ItemFrame(tris, rot_deg).apply(tris)


def _blade_split(obj_path, group="blade", tang_x1=14.0):
    """取一条刀身（含刀尾"茎"），返回 (舞台三角, 材质表根部 UV 块)。

    tang_x1 控制刀尾长度：直接手持留长尾，被钳子夹着那份截到钳口尖端。"""
    tris, rect = _collect(obj_path, group, True)
    tris = tris + tang_box(rect, x1=tang_x1)
    return tris


def split_hold_parts():
    """手持部件：金属刀条=大太刀刀刃、木／竹刀条=木偶／竹光刀刃、
    刀鞘=**木偶的刀鞘**（blade.obj 的 sheath 分组 + 木偶 wood.png 材质表），
    外加"钳子夹工件"的组合件。

    刀条带一截延长的刀尾（茎）——和组装动画里那根一样，握在手里能看见刀尾；
    被钳子夹着的那份刀尾截短到钳口尖端，不然会从钳臂中间穿出去。

    锚点规则：直接手持时以**工件自己**的包围盒为中心；被钳子夹着时以
    **钳子本身**的包围盒为中心——原版 display 是绕立方体中心旋转的，锚点落在
    钳子中心，手就正好握在钳身（铆钉到钳柄那一段）上，而不是掐在刀条中段。"""
    metal = _blade_split(ODACHI_OBJ)
    metal_grip = _blade_split(ODACHI_OBJ, tang_x1=7.0)
    wood = _blade_split(OBJ)
    sheath = _collect(OBJ, "sheath", False)[0]
    ingot = _ingot()
    # 钳口内侧面贴着工件表面合拢：刀尾半高 0.0089 → gap 0.0095；
    # 锭子半高 0.025 → gap 0.0265。钳口长度盖过工件尾端（刀尾止于 0.127、
    # 锭子止于 0.160），钳臂从钳口尖端才起步——穿模就是这么根治的。
    tongs_blade = _tongs(gap=0.0045, jaw_len=0.135, z_jaw=0.010, axis="z")
    tongs_ingot = _tongs(gap=0.024, jaw_len=0.170, z_jaw=0.026, axis="z")

    plain = _ItemFrame(metal, HOLD_BLADE_ROT)
    gripped = _ItemFrame(tongs_blade, HOLD_BLADE_ROT)
    ingot_plain = _ItemFrame(ingot, HOLD_BLADE_ROT)
    ingot_grip = _ItemFrame(tongs_ingot, HOLD_BLADE_ROT)
    sheath_frame = _ItemFrame(sheath, HOLD_SHEATH_ROT)

    return {
        "hold_blade_metal": plain.apply(metal),
        "hold_blade_wood": plain.apply(wood),
        "hold_blade_bamboo": plain.apply(wood),
        "hold_sheath": sheath_frame.apply(sheath),
        "hold_blade_grip": gripped.apply(metal_grip),
        "hold_tongs_blade": gripped.apply(tongs_blade),
        "hold_ingot": ingot_plain.apply(ingot),
        "hold_ingot_flat": _ingot_flat(),
        "hold_ingot_grip": ingot_grip.apply(ingot),
        "hold_tongs_ingot": ingot_grip.apply(tongs_ingot),
    }


# --------------------------------------------------------------------------
# 导出 1：游戏内 JSON（每部件一个浮点数组：[x,y,z,u,v] * 3 顶点/三角）
# --------------------------------------------------------------------------

def export_json(parts, hold):
    doc = {"version": 3,
           "note": "由 animation_tools/build_parts_model.py 从拔刀剑本体 jar 切分生成；勿手改，重跑脚本即可。"
                   "blade_blank/tsuba/handle/sheath 是组装动画部件（舞台坐标，原点在刀镡中心）；"
                   "hold_* 是手持物品模型部件（物品模型坐标，0..1 立方）",
           "parts": {}}
    for name in PART_ORDER:
        arr = []
        for (p, uv) in parts[name]:
            arr += [round(c, 5) for c in p] + [round(uv[0], 5), round(uv[1], 5)]
        doc["parts"][name] = arr
    for name, tris in hold.items():
        arr = []
        for (p, uv) in tris:
            arr += [round(c, 5) for c in p] + [round(uv[0], 5), round(uv[1], 5)]
        doc["parts"][name] = arr
        xs = [t[0][0] for t in tris]; ys = [t[0][1] for t in tris]; zs = [t[0][2] for t in tris]
        print("  %-18s %4d tris  x[%.3f,%.3f] y[%.3f,%.3f] z[%.3f,%.3f]" % (
            name, len(tris) // 3, min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(doc, f, separators=(",", ":"))
    print("JSON ->", OUT_JSON, os.path.getsize(OUT_JSON), "bytes")


def _tile_h(img, n):
    w, h = img.size
    out = Image.new(img.mode, (w * n, h))
    for i in range(n):
        out.paste(img, (i * w, 0))
    return out


def _opaque(img):
    """把全透明像素用同列最近的实心像素填充（拔刀剑材质表上半部有大片透明区，
    CLAMP 采样下边缘面会透出背景；填充后任何查看器都不再出现空洞/拉丝）。
    顺带抹掉材质表右上角残留的青/品红标定像素（会被补色拉成蓝点）。"""
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a >= 128 and ((b > 180 and g > 140 and r < 120) or (r > 170 and b > 90 and g < 100)):
                px[x, y] = (0, 0, 0, 0)
    for x in range(w):
        last = None
        for y in range(h):
            r, g, b, a = px[x, y]
            if a >= 128:
                last = (r, g, b, 255)
            elif last:
                px[x, y] = last
        last = None
        for y in range(h - 1, -1, -1):
            r, g, b, a = px[x, y]
            if a >= 128:
                last = (r, g, b, 255)
            elif last:
                px[x, y] = last
    return img


def export_textures():
    os.makedirs(OUT_TEX_DIR, exist_ok=True)
    zf = zipfile.ZipFile(JAR)
    out = {}
    for src, dst in ((TEX_WOOD, "sb_wood.png"), (TEX_BAMBOO, "sb_bamboo.png"),
                     (TEX_ODACHI, "sb_oodachi.png")):
        im = Image.open(io.BytesIO(zf.read(src))).convert("RGBA")
        im = _opaque(_tile_h(im, TILE_U))
        im.save(os.path.join(OUT_TEX_DIR, dst))
        out[dst] = im
    # sb_iron.png / sb_hot.png 是画好的整块料（钳子、烧红锭子），这里只做一次
    # "上亮下暗"的校验式整理：全透明处填实、并保证四边不透明，免得盒子边缘露底。
    for dst in ("sb_iron.png", "sb_hot.png"):
        path = os.path.join(OUT_TEX_DIR, dst)
        im = Image.open(path).convert("RGBA")
        out[dst] = _opaque(im)
        out[dst].save(path)
    print("TEX  ->", OUT_TEX_DIR, "(横向平铺 %d 份, %dx%d)" % (TILE_U, out["sb_wood.png"].size[0], out["sb_wood.png"].size[1]))
    return out


# --------------------------------------------------------------------------
# 导出 2：PMX（真网格四部件 + 四骨骼，供 MMD / Blender mmd_tools 预览）
# --------------------------------------------------------------------------

def _s(b):
    e = b.encode("utf-16-le", errors="replace")
    return struct.pack("<i", len(e)) + e


def _face_normal(tri):
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = tri
    e1 = (bx - ax, by - ay, bz - az)
    e2 = (cx - ax, cy - ay, cz - az)
    n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2],
         e1[0] * e2[1] - e1[1] * e2[0])
    ln = math.sqrt(sum(c * c for c in n)) or 1.0
    return tuple(c / ln for c in n)


def export_pmx(parts):
    # PMX 用 MMD 坐标（Y 下）：与舞台坐标差一个 Y 取反
    def to_mmd(p):
        return (p[0], -p[1], p[2])

    buf = bytearray()
    buf += b"PMX File Format\x00"
    buf += struct.pack("<f", 2.0)
    buf += struct.pack("<B", 8)
    buf += struct.pack("<8B", 8, 0, 11, 11, 11, 11, 11, 11)
    buf += _s("刀条组装部件") + _s("blade_assembly_parts")
    buf += _s("拔刀剑 blade.obj 切分四部件 (8单位=1格, MMD Y-down)") + _s("")

    all_verts, all_tris = [], []
    tex = "../src/main/resources/assets/%s/textures/assembly/sb_wood.png" % NS
    vert_count, index_lists = 0, []
    for name in PART_ORDER:
        tris = parts[name]
        idx = []
        for k in range(0, len(tris), 3):
            tri = [tris[k + j] for j in range(3)]
            n = _face_normal([to_mmd(t[0]) for t in tri])
            for (p, uv) in tri:
                all_verts.append((to_mmd(p), n, uv, name))
                idx.append(vert_count)
                vert_count += 1
        index_lists.append(idx)
    # 骨权重索引
    bone_names = PART_ORDER
    buf += struct.pack("<i", len(all_verts))
    for (p, n, uv, bone) in all_verts:
        buf += struct.pack("<3f", *p) + struct.pack("<3f", *n) + struct.pack("<2f", *uv)
        buf += struct.pack("<B", 0) + struct.pack("<i", bone_names.index(bone))
        buf += struct.pack("<B", 0) + struct.pack("<f", 1.0)
    total_idx = sum(len(i) for i in index_lists)
    buf += struct.pack("<i", total_idx)
    for idx in index_lists:
        for i in idx:
            buf += struct.pack("<i", i)
    buf += struct.pack("<i", len(PART_ORDER))
    for name in PART_ORDER:
        buf += struct.pack("<4f", 1, 1, 1, 1) + struct.pack("<3f", 0.1, 0.1, 0.1)
        buf += struct.pack("<3f", 0.4, 0.4, 0.4) + struct.pack("<f", 5.0)
        buf += struct.pack("<B", 0x20) + struct.pack("<B", 0)
        buf += struct.pack("<4f", 0, 0, 0, 1) + struct.pack("<f", 1.0)
        buf += struct.pack("<B", 1) + struct.pack("<i", 0)
        buf += struct.pack("<i", -1)
        buf += _s(PART_CN[name])
    # 骨骼：center + 4 部件骨（主名与 VMD 一致）
    buf += struct.pack("<i", 5)

    def bone_entry(name, ename, pos, parent, child_off):
        b = _s(name) + _s(ename) + struct.pack("<3f", *pos)
        b += struct.pack("<h", parent) + struct.pack("<i", 0)
        b += struct.pack("<H", 0x004E) + struct.pack("<3f", *child_off)
        return b
    buf += bone_entry("中心", "center", (0, 0, 0), -1, (0, 0, 0))
    for name in PART_ORDER:
        buf += bone_entry(name, PART_CN[name], (0, 0, 0), 0, (0, 0.5, 0))
    buf += struct.pack("<i", 0) * 2 + struct.pack("<i", 0) * 2
    buf += struct.pack("<i", 1) + _s(tex) + struct.pack("<i", 0)
    os.makedirs(os.path.dirname(OUT_PMX), exist_ok=True)
    with open(OUT_PMX, "wb") as f:
        f.write(buf)
    print("PMX  ->", OUT_PMX, len(buf), "bytes")


# --------------------------------------------------------------------------
# 导出 3：glTF 二进制（.glb）：四部件网格 + 组装动画，Blender 直接 File>Import
# --------------------------------------------------------------------------

def export_glb(parts, tracks, tex_png_bytes):
    bin_chunks = []
    bin_len = 0

    def add_view(data, target):
        nonlocal bin_len
        if isinstance(data, (list, tuple)):
            if target == 34962:  # ARRAY_BUFFER float
                data = struct.pack("<%df" % len(data), *data)
            elif target == 34963:  # ELEMENT_ARRAY_BUFFER uint16
                data = struct.pack("<%dH" % len(data), *data)
            data = bytes(data)
        pad = (4 - bin_len % 4) % 4
        if pad:
            bin_chunks.append(b"\x00" * pad)
            bin_len += pad
        off = bin_len
        bin_chunks.append(data)
        bin_len += len(data)
        pad = (4 - bin_len % 4) % 4
        if pad:
            bin_chunks.append(b"\x00" * pad)
            bin_len += pad
        view = {"buffer": 0, "byteOffset": off, "byteLength": len(data)}
        if target:
            view["target"] = target
        buffer_views.append(view)
        return len(buffer_views) - 1

    accessors, buffer_views, meshes, nodes = [], [], [], []
    for name in PART_ORDER:
        tris = parts[name]
        pos, uv, idx = [], [], []
        mn = [1e9, 1e9, 1e9]
        mx = [-1e9, -1e9, -1e9]
        for k, (p, u) in enumerate(tris):
            pos += list(p)
            uv += [u[0], u[1]]
            mn = [min(a, b) for a, b in zip(mn, p)]
            mx = [max(a, b) for a, b in zip(mx, p)]
            idx.append(k)
        pv = add_view(pos, 34962)
        iv = add_view(idx, 34963)
        uv_v = add_view(uv, 34962)
        pa = len(accessors)
        accessors.append({"bufferView": pv, "componentType": 5126, "count": len(tris),
                          "type": "VEC3", "min": mn, "max": mx})
        ia = len(accessors)
        accessors.append({"bufferView": iv, "componentType": 5123, "count": len(tris), "type": "SCALAR"})
        ua = len(accessors)
        accessors.append({"bufferView": uv_v, "componentType": 5126, "count": len(tris), "type": "VEC2"})
        meshes.append({"name": PART_CN[name], "primitives": [{
            "attributes": {"POSITION": pa, "TEXCOORD_0": ua}, "indices": ia, "material": 0, "mode": 4}]})
        nodes.append({"name": name, "mesh": len(meshes) - 1, "translation": [0, 0, 0]})

    png = tex_png_bytes  # 与 JSON/PMX 同一张横向平铺后的材质表（UV 已按 /TILE_U 归一）
    img_view = add_view(png, None)  # add_view 已保证 4 字节对齐

    # 动画：每部件 translation + rotation(Z)，30fps 稠密帧
    samplers, channels = [], []
    times = [f / 30.0 for f in range(121)]
    tv = add_view(times, 34962)
    ta = len(accessors)
    accessors.append({"bufferView": tv, "componentType": 5126, "count": len(times),
                      "type": "SCALAR", "min": [times[0]], "max": [times[-1]]})
    for node_i, (bone, dense) in enumerate(tracks):
        trans, rots = [], []
        for f, p, roll in dense:
            trans += [p[0], p[1], p[2]]
            h = math.radians(roll) * 0.5
            rots += [0.0, 0.0, math.sin(h), math.cos(h)]
        trv = add_view(trans, 34962)
        tra = len(accessors)
        accessors.append({"bufferView": trv, "componentType": 5126, "count": len(dense), "type": "VEC3"})
        rov = add_view(rots, 34962)
        roa = len(accessors)
        accessors.append({"bufferView": rov, "componentType": 5126, "count": len(dense), "type": "VEC4"})
        samplers.append({"input": ta, "output": tra, "interpolation": "LINEAR"})
        channels.append({"sampler": len(samplers) - 1, "target": {"node": node_i, "path": "translation"}})
        samplers.append({"input": ta, "output": roa, "interpolation": "LINEAR"})
        channels.append({"sampler": len(samplers) - 1, "target": {"node": node_i, "path": "rotation"}})

    gltf = {
        "asset": {"version": "2.0", "generator": "BladeForgeWorkshop build_parts_model.py"},
        "scene": 0,
        "scenes": [{"nodes": list(range(len(nodes))), "name": "blade_assembly"}],
        "nodes": nodes,
        "meshes": meshes,
        "materials": [{"name": "sb_material_sheet", "doubleSided": True,
                       "pbrMetallicRoughness": {"baseColorTexture": {"index": 0},
                                                "metallicFactor": 0.0, "roughnessFactor": 0.85}}],
        "textures": [{"sampler": 0, "source": 0}],
        "samplers": [{"magFilter": 9728, "minFilter": 9728,  # NEAREST：像素材质表不糊
                      "wrapS": 33071, "wrapT": 33071}],      # CLAMP_TO_EDGE
        "images": [{"bufferView": img_view, "mimeType": "image/png"}],
        "accessors": accessors,
        "bufferViews": buffer_views,
        "animations": [{"name": "blade_assembly", "samplers": samplers, "channels": channels}],
        "buffers": [{"byteLength": bin_len}],
    }
    json_blob = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    json_pad = (4 - len(json_blob) % 4) % 4
    json_blob += b" " * json_pad
    bin_blob = b"".join(bin_chunks)
    total = 12 + 8 + len(json_blob) + 8 + len(bin_blob)
    out = bytearray()
    out += struct.pack("<3I", 0x46546C67, 2, total)
    out += struct.pack("<2I", len(json_blob), 0x4E4F534A) + json_blob
    out += struct.pack("<2I", len(bin_blob), 0x004E4942) + bin_blob
    os.makedirs(os.path.dirname(OUT_GLB), exist_ok=True)
    with open(OUT_GLB, "wb") as f:
        f.write(out)
    print("GLB  ->", OUT_GLB, len(out), "bytes")


if __name__ == "__main__":
    import assembly_vmd
    parts = split_parts()
    for name in PART_ORDER:
        print(f"  {PART_CN[name]:3s} {name:12s} {len(parts[name]) // 3:4d} tris")
    hold = split_hold_parts()
    for name in HOLD_PART_CN:
        print(f"  手持 {HOLD_PART_CN[name]:16s} {name}")
    export_json(parts, hold)
    tiled = export_textures()
    buf = io.BytesIO()
    tiled["sb_wood.png"].save(buf, format="PNG")
    export_pmx(parts)
    export_glb(parts, assembly_vmd.build_stage_tracks(), buf.getvalue())
    print("DONE")
