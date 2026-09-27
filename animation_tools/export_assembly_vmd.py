# -*- coding: utf-8 -*-
"""
export_assembly_vmd.py —— 把 Blender 里改完动画后导出的 blade_assembly.glb
重新写回游戏用的 blade_assembly.vmd（build_parts_model.py 的逆操作）。

工作流（不需要任何插件）:
  1. Blender → 文件 → 导入 → glTF 2.0，选 blender_preview/blade_assembly.glb
     （四个部件网格 + 一条名为 blade_assembly 的动画）
  2. 在动作编辑器里改关键帧（部件横放、刀尖朝左；X 右 Y 上 Z 朝屏幕外，1 单位 = 1 格）
  3. 文件 → 导出 → glTF 2.0，覆盖 blender_preview/blade_assembly.glb
     （导出面板里勾选 "Animation"，格式保持 glb）
  4. python3 animation_tools/export_assembly_vmd.py
     → 自动写回 src/main/resources/.../assembly/blade_assembly.vmd，重新打包即生效

glTF 与舞台坐标同轴（X 右、Y 上、Z 朝屏幕外、米=格），直接采样即可；
写盘时由 assembly_vmd.write_vmd 转成 MMD 约定。
"""
import json
import math
import os
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

import assembly_vmd  # noqa: E402

GLB_PATH = os.path.join(ROOT, "blender_preview", "blade_assembly.glb")
DEFAULT_OUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                           "slashbladeresh_slashblad", "assembly", "blade_assembly.vmd")
TOTAL_FRAMES = 120
BONE_ORDER = ["blade_blank", "tsuba", "handle", "sheath"]


def read_glb(path):
    raw = open(path, "rb").read()
    magic, ver, total = struct.unpack("<3I", raw[:12])
    assert magic == 0x46546C67, "不是 glb 文件"
    off = 12
    json_blob = None
    bin_blob = None
    while off < len(raw):
        clen, ctype = struct.unpack("<2I", raw[off:off + 8])
        body = raw[off + 8:off + 8 + clen]
        if ctype == 0x4E4F534A:
            json_blob = json.loads(body.decode("utf-8"))
        elif ctype == 0x004E4942:
            bin_blob = body
        off += 8 + clen
    assert json_blob is not None and bin_blob is not None, "glb 缺少 JSON/BIN 块"
    return json_blob, bin_blob


def accessor_data(gltf, blob, acc_index):
    acc = gltf["accessors"][acc_index]
    view = gltf["bufferViews"][acc["bufferView"]]
    comp = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[acc["type"]]
    fmt, size = {5126: ("f", 4), 5123: ("H", 2), 5125: ("I", 4), 5121: ("B", 1),
                 5122: ("h", 2), 5120: ("b", 1)}[acc["componentType"]]
    base = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
    count = acc["count"] * comp
    step = size + view.get("byteStride", 0)
    out = []
    for i in range(count):
        out.append(struct.unpack_from("<" + fmt, blob, base + i * step)[0])
    return out


def sample_channel(times, values, comp, t):
    """LINEAR 采样一条动画通道。"""
    if t <= times[0]:
        return values[0:comp]
    if t >= times[-1]:
        return values[(len(times) - 1) * comp: len(times) * comp]
    lo = 0
    while lo + 1 < len(times) and times[lo + 1] <= t:
        lo += 1
    t0, t1 = times[lo], times[lo + 1]
    k = (t - t0) / (t1 - t0) if t1 > t0 else 0.0
    a = values[lo * comp:(lo + 1) * comp]
    b = values[(lo + 1) * comp:(lo + 2) * comp]
    return [a[i] + (b[i] - a[i]) * k for i in range(comp)]


def main():
    glb = sys.argv[1] if len(sys.argv) > 1 else GLB_PATH
    out = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_OUT
    gltf, blob = read_glb(glb)
    anims = gltf.get("animations") or []
    if not anims:
        print("!! glb 里没有动画通道（Blender 导出面板请勾选 Animation）")
        sys.exit(1)
    anim = anims[0]
    nodes = gltf["nodes"]
    per_bone = {}   # bone -> {"translation": (times, vals, comp), "rotation": ...}
    for ch in anim["channels"]:
        node = nodes[ch["target"]["node"]]
        bone = node.get("name")
        if bone not in BONE_ORDER:
            continue
        s = anim["samplers"][ch["sampler"]]
        comp = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[gltf["accessors"][s["output"]]["type"]]
        times = accessor_data(gltf, blob, s["input"])
        vals = accessor_data(gltf, blob, s["output"])
        per_bone.setdefault(bone, {})[ch["target"]["path"]] = (times, vals, comp)

    tracks = []
    for bone in BONE_ORDER:
        if bone not in per_bone:
            print("!! glb 里缺少部件", bone, "的动画通道")
            sys.exit(1)
        dense = []
        for f in range(TOTAL_FRAMES + 1):
            t = f / 30.0
            tr = per_bone[bone].get("translation")
            x, y, z = sample_channel(*tr, t) if tr else (0.0, 0.0, 0.0)
            ro = per_bone[bone].get("rotation")
            roll = 0.0
            if ro:
                q = sample_channel(*ro, t)
                n = math.sqrt(sum(c * c for c in q)) or 1.0
                # 只取绕 Z 分量（组装动画约定不做空间旋转）
                roll = math.degrees(2.0 * math.atan2(q[2] / n, q[3] / n))
            dense.append((f, (x, y, z), roll))
        tracks.append((bone, dense))

    size, keys = assembly_vmd.write_vmd(out, tracks)
    print(f"OK {out}  {size} bytes, {keys} keyframes")


if __name__ == "__main__":
    main()
