# -*- coding: utf-8 -*-
"""
assembly_vmd.py —— 「刀条/刀镡/刀柄/刀鞘」横放组装动画的编排与 VMD 写出。

时间轴与游戏服务端 BladeAssembly.java 完全对齐：
  30fps，120 帧 = 80 游戏刻（1 刻 = 1.5 帧）
  刀镡落座  f42 (=刻28)   刀柄落座  f75 (=刻50)   入鞘落座  f99 (=刻66)

编排流程（用户约定，全部横放，刀尖朝左、刀柄朝右）：
  f0-12   刀条横着从画面右侧飞入，停在舞台中央（骨原点 = 刀镡中心）
  f12-42  刀镡从刀条右侧（画面右）滑来，略微多穿过去一点再回位——
          刀条的茎（右侧多出的部分）穿过刀镡
  f42-75  横着的刀柄从画面右滑来，盖住多出来的茎，撞到刀镡后回位
  f75-99  刀鞘从刀条左侧（画面左）横着出现，向右平移罩住刀条
  f99-120 整刀保持横放微微起伏，客户端在收尾阶段做缩入背包的特效

坐标系约定：
  build_stage_tracks() 产出「舞台坐标」：X 屏幕右、Y 屏幕上、Z 朝摄像机，单位 = 格。
  所有骨骼的"落座位"都是 (0,0,0)：部件网格顶点本身就在整刀坐标系里
  （见 build_parts_model.py），所以只要骨骼回零，四部件必然严丝合缝。
  写盘时转成 MMD 标准 VMD 约定：Y 轴朝下（pos_y 取反），绕 Z 正=屏幕顺时针，
  8 VMD 单位 = 1 格。游戏客户端 AssemblyMotion.java 采样时再换算回舞台坐标。

用法: python3 assembly_vmd.py [输出路径]
默认输出到 src/main/resources/assets/slashbladeresh_slashblad/assembly/blade_assembly.vmd
"""
import math
import os
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DEFAULT_OUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                           "slashbladeresh_slashblad", "assembly", "blade_assembly.vmd")

TOTAL_FRAMES = 120
U = 8.0  # 每格 8 个 VMD 单位

BONE_ORDER = ["blade_blank", "tsuba", "handle", "sheath"]


def smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3.0 - 2.0 * t)


def ease_out_back(t, k=1.05):
    """末端轻微过冲再回位，模拟"咔哒"落座的顿挫。"""
    t = min(1.0, max(0.0, t))
    s = t * t * (3 - 2 * t)
    return s + k * math.sin(math.pi * t) * (1 - s) * 0.12


def bob(f, amp=0.006, period=26.0):
    return amp * math.sin(2.0 * math.pi * f / period)


def seg(f, f0, f1):
    return smooth((f - f0) / float(f1 - f0)) if f1 > f0 else 1.0


def park(f, start, pos, roll):
    """部件未登场时停在画面外（start 之前保持同一停放位）。"""
    return (pos, roll) if f < start else None


# --------------------------------------------------------------------------
# 编排：每帧稠密采样，返回 [(bone, [(frame, (x,y,z), roll_deg)])]，舞台坐标
# --------------------------------------------------------------------------

def build_stage_tracks():
    tracks = []

    # ---- 刀条：横着从画面右飞入，f12 停稳，之后原地轻微起伏 ----
    dense = []
    for f in range(TOTAL_FRAMES + 1):
        t = seg(f, 0, 12)
        x = 1.62 * (1.0 - t)
        y = -0.10 * (1.0 - t) + (bob(f) if f >= 12 else 0.0)
        z = 0.10 * (1.0 - t)
        r = 14.0 * (1.0 - t)
        dense.append((f, (x, y, z), r))
    tracks.append(("blade_blank", dense))

    # ---- 刀镡：f12 从画面右（刀条右侧）出现，向左平移；
    #      f36 略微多穿过一点（x 过冲到 -0.07），f42 回位落座 ----
    dense = []
    for f in range(TOTAL_FRAMES + 1):
        if f < 12:
            x, y, z, r = 2.30, 0.12, 0.06, 18.0
        elif f <= 36:
            t = ease_out_back((f - 12) / 24.0)
            x = 2.30 + (-0.07 - 2.30) * t
            y = 0.12 * (1.0 - t)
            z = 0.06 * (1.0 - t)
            r = 18.0 * (1.0 - seg(f, 12, 30))
        elif f <= 42:
            t = seg(f, 36, 42)
            x = -0.07 * (1.0 - t)          # 从"穿过一点"回正到落座位
            y, z = 0.0, 0.0
            r = 0.0
        else:
            x, y, z, r = 0.0, bob(f), 0.0, 0.0
        dense.append((f, (x, y, z), r))
    tracks.append(("tsuba", dense))

    # ---- 刀柄：f42 从画面右横着出现，向左靠拢，f69 抵住刀镡（过冲 -0.03），
    #      f75 回位，把刀条多出的茎整个罩住 ----
    dense = []
    for f in range(TOTAL_FRAMES + 1):
        if f < 42:
            x, y, z, r = 2.30, 0.10, 0.05, 12.0
        elif f <= 69:
            t = ease_out_back((f - 42) / 27.0)
            x = 2.30 + (-0.03 - 2.30) * t
            y = 0.10 * (1.0 - t)
            z = 0.05 * (1.0 - t)
            r = 12.0 * (1.0 - seg(f, 42, 62))
        elif f <= 75:
            t = seg(f, 69, 75)
            x = -0.03 * (1.0 - t)
            y, z = 0.0, 0.0
            r = 0.0
        else:
            x, y, z, r = 0.0, bob(f), 0.0, 0.0
        dense.append((f, (x, y, z), r))
    tracks.append(("handle", dense))

    # ---- 刀鞘：f75 在刀条左侧横着出现，向右平移罩住刀条；
    #      滑行时略靠前（z+），f96 过冲到位，f99 落座并回落到与刀条同平面 ----
    dense = []
    for f in range(TOTAL_FRAMES + 1):
        if f < 75:
            x, y, z, r = -2.30, -0.10, 0.06, -10.0
        elif f <= 96:
            t = ease_out_back((f - 75) / 21.0)
            x = -2.30 + (0.04 + 2.30) * t   # 向右平移，末端略微过冲
            y = -0.10 * (1.0 - t)
            z = 0.06 + (0.012 - 0.06) * t   # 从靠前逐渐贴回
            r = -10.0 * (1.0 - seg(f, 75, 90))
        elif f <= 99:
            t = seg(f, 96, 99)
            x = 0.04 * (1.0 - t)
            y, z, r = 0.0, 0.0, 0.0
        else:
            x, y, z, r = 0.0, bob(f) * 0.5, 0.0, 0.0
        dense.append((f, (x, y, z), r))
    tracks.append(("sheath", dense))
    return tracks


# --------------------------------------------------------------------------
# 舞台坐标 -> MMD VMD 并写盘
# --------------------------------------------------------------------------

def encode_sjis(name, length):
    b = name.encode("shift_jis", errors="replace")[:length]
    return b + b"\x00" * (length - len(b))


def stage_to_mmd(x, y, z, roll_deg):
    """舞台(右/上/朝屏幕外, 格, 逆时针为正) -> MMD(右/下/朝屏幕外, 8单位/格, 顺时针为正)。"""
    r = math.radians(roll_deg) * 0.5
    return (x * U, -y * U, z * U), (0.0, 0.0, math.sin(-r), math.cos(-r))


def write_vmd(path, stage_tracks, model_name="blade_assembly_parts"):
    bone_tracks = []
    for name, dense in stage_tracks:
        keys = []
        for f, pos, roll in dense:
            p, q = stage_to_mmd(pos[0], pos[1], pos[2], roll)
            keys.append((f, p, q))
        bone_tracks.append((name, keys))
    buf = bytearray()
    header = b"Vocaloid Motion Data 0002"
    buf += header + b"\x00" * (30 - len(header))
    buf += encode_sjis(model_name, 20)
    total = sum(len(k) for _, k in bone_tracks)
    buf += struct.pack("<I", total)
    interp = bytes([0x14] * 64)
    for name, keys in bone_tracks:
        for f, pos, rot in keys:
            buf += encode_sjis(name, 15)
            buf += struct.pack("<I", int(f))
            buf += struct.pack("<3f", *pos)
            buf += struct.pack("<4f", *rot)
            buf += interp
    for _ in range(4):
        buf += struct.pack("<I", 0)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(buf)
    return len(buf), total


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    size, keys = write_vmd(out, build_stage_tracks())
    print(f"OK {out}  {size} bytes, {keys} keyframes")
