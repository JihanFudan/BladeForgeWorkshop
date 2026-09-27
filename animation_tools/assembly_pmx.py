# -*- coding: utf-8 -*-
"""
assembly_pmx.py —— 生成「刀条/刀镡/刀柄/刀鞘」四部件的 PMX 模型（VMD 配套模型）。

- 坐标系：MMD 标准（X 右、Y 下、Z 朝屏幕外），8 单位 = 1 格，
  与 assembly_vmd.py 写出的 blade_assembly.vmd 骨骼名一一对应：
  blade_blank / tsuba / handle / sheath。
- 用途：在 MMD 或 Blender(mmd_tools) 里加载本模型 + blade_assembly.vmd
  即可预览组装动画；游戏内不需要本文件（游戏用物品图标 + VMD 轨迹）。
- 贴图直接引用模组里的物品贴图（相对路径）。

用法: python3 assembly_pmx.py [输出路径]
默认输出 blender_preview/blade_assembly_parts.pmx
"""
import os
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DEFAULT_OUT = os.path.join(ROOT, "blender_preview", "blade_assembly_parts.pmx")

TEX_DIR = "../src/main/resources/assets/slashbladeresh_slashblad/textures/item"
U = 8.0  # 每格 8 单位

# 部件：名字 / 骨骼名 / 贴图 / 盒尺寸(格: 长Y 宽X 厚Z) / 骨原点(MMD Y下, Z朝外)
PARTS = [
    ("刀条", "blade_blank", f"{TEX_DIR}/wooden_blade_blank.png", (0.45, 0.06, 0.014), (0.0, -0.225, 0.10)),
    ("刀镡", "tsuba",       f"{TEX_DIR}/tsuba_wood_iron.png",    (0.20, 0.20, 0.030), (0.0, 0.000, 0.16)),
    ("刀柄", "handle",      f"{TEX_DIR}/blade_handle.png",       (0.30, 0.05, 0.050), (0.0, 0.200, 0.12)),
    ("刀鞘", "sheath",      f"{TEX_DIR}/blade_sheath.png",       (0.55, 0.06, 0.060), (0.0, -0.275, 0.22)),
]


def s(b):  # utf16le string
    e = b.encode("utf-16-le", errors="replace")
    return struct.pack("<i", len(e)) + e


def box(lx, ly, lz):
    """8 顶点 / 12 三角形 / 每面独立顶点(24v)，法线朝外。返回 verts, tris。"""
    hx, hy, hz = lx / 2, ly / 2, lz / 2
    faces = [
        ((0, 0, 1), [(-hx, -hy, hz), (hx, -hy, hz), (hx, hy, hz), (-hx, hy, hz)]),
        ((0, 0, -1), [(hx, -hy, -hz), (-hx, -hy, -hz), (-hx, hy, -hz), (hx, hy, -hz)]),
        ((1, 0, 0), [(hx, -hy, hz), (hx, -hy, -hz), (hx, hy, -hz), (hx, hy, hz)]),
        ((-1, 0, 0), [(-hx, -hy, -hz), (-hx, -hy, hz), (-hx, hy, hz), (-hx, hy, -hz)]),
        ((0, 1, 0), [(-hx, hy, hz), (hx, hy, hz), (hx, hy, -hz), (-hx, hy, -hz)]),
        ((0, -1, 0), [(-hx, -hy, -hz), (hx, -hy, -hz), (hx, -hy, hz), (-hx, -hy, hz)]),
    ]
    uvs = [(0, 1), (1, 1), (1, 0), (0, 0)]
    verts, tris, idx = [], [], 0
    for n, quad in faces:
        for i in range(4):
            verts.append((quad[i], n, uvs[i]))
        tris += [(idx, idx + 1, idx + 2), (idx, idx + 2, idx + 3)]
        idx += 4
    return verts, tris


def build():
    buf = bytearray()
    buf += b"PMX File Format\x00"
    buf += struct.pack("<f", 2.0)
    buf += struct.pack("<B", 8)               # 附加数据数量
    buf += struct.pack("<8B", 8, 0, 11, 11, 11, 11, 11, 11)
    # 语义: 编码=0(UTF-16LE) 追加UV=0 顶点/纹理/材质/骨骼/变形/框架索引=4字节
    buf += s("刀条组装部件") + s("blade_assembly_parts")
    buf += s("AutoMods 重锋 组装动画部件模型 (8单位=1格, MMD Y-down)") + s("")

    all_verts, all_tris, materials, textures = [], [], [], []
    for (cname, bone, tex, size, origin) in PARTS:
        base = len(all_verts)
        lx, ly, lz = (v * U for v in size)
        verts, tris = box(lx, ly, lz)
        ox, oy, oz = (v * U for v in origin)
        for (p, n, uv) in verts:
            all_verts.append((p, n, uv, bone))  # 骨权重稍后按名字解析
        for t in tris:
            all_tris.append(tuple(i + base for i in t))
        textures.append(tex)
        materials.append(len(textures) - 1)

    # 顶点
    bone_names = [p[1] for p in PARTS]
    buf += struct.pack("<i", len(all_verts))
    for (p, n, uv, bone) in all_verts:
        buf += struct.pack("<3f", *p) + struct.pack("<3f", *n) + struct.pack("<2f", *uv)
        buf += struct.pack("<B", 0) + struct.pack("<i", bone_names.index(bone))
        buf += struct.pack("<B", 0) + struct.pack("<f", 1.0)  # no edge
    # 面（索引宽度按头部声明 = 4 字节，无类型字节）
    buf += struct.pack("<i", len(all_tris) * 3)
    for t in all_tris:
        buf += struct.pack("<3i", *t)
    # 材质
    buf += struct.pack("<i", len(PARTS))
    for i, (cname, bone, tex, size, origin) in enumerate(PARTS):
        buf += struct.pack("<4f", 1, 1, 1, 1)          # diffuse
        buf += struct.pack("<3f", 0.1, 0.1, 0.1)       # ambient
        buf += struct.pack("<3f", 0.4, 0.4, 0.4)       # specular
        buf += struct.pack("<f", 5.0)                  # shine
        buf += struct.pack("<B", 0x20)                 # 贴图+球
        buf += struct.pack("<B", 0)                    # 无边
        buf += struct.pack("<4f", 0, 0, 0, 1) + struct.pack("<f", 1.0)
        buf += struct.pack("<B", 1) + struct.pack("<i", i)   # 贴图
        buf += struct.pack("<i", -1)                          # 共享toon
        buf += s(cname)
    # 骨骼：center + 4 部件骨
    buf += struct.pack("<i", 5)
    def bone_entry(name, ename, pos, parent, child_off):
        b = s(name) + s(ename) + struct.pack("<3f", *pos)
        b += struct.pack("<h", parent) + struct.pack("<i", 0)
        b += struct.pack("<H", 0x004E)   # 可旋转|可移动|可显示|可操作|影响父骨骼；子骨=偏移
        b += struct.pack("<3f", *child_off)
        return b
    buf += bone_entry("中心", "center", (0, 0, 0), -1, (0, 0, 0))
    for (cname, bone, tex, size, origin) in PARTS:
        ox, oy, oz = (v * U for v in origin)
        # 主名必须与 VMD 骨骼名一致（blade_blank 等），显示名放英文名位
        buf += bone_entry(bone, cname, (0, oy, oz), 0, (0, 0.5, 0))
    # 变形/框架/刚体/关节/toon表 全空
    buf += struct.pack("<i", 0)   # morphs
    buf += struct.pack("<i", 0)   # frames
    buf += struct.pack("<i", 0)   # rigid bodies
    buf += struct.pack("<i", 0)   # joints
    buf += struct.pack("<i", len(textures))
    for t in textures:
        buf += s(t)
    buf += struct.pack("<i", 0)   # 共享 toon
    return bytes(buf)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    data = build()
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(data)
    print(f"OK {out}  {len(data)} bytes")
