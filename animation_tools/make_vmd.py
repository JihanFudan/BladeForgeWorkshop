# -*- coding: utf-8 -*-
"""
make_vmd.py v2 — 完整动画版生成器（VMD 骨骼帧 = 111 字节：15名+4帧+12pos+16rot+64插值）
1. inspect_a.vmd   = 参考项目 inspect_a.vmd 原样复制（通用，完整 47 帧）
2. inspect_yamato.vmd = 参考完整动画变体：拔刀段（f79-f96）刀改为竖直拔出
   竖立 rot = q_v = (0,0,-0.7071,0.7071)（相机空间刀尖朝上，已验证）
   位置 pos 全部保持参考原值（严格照抄参考坐标），f101 之后参考原样
   （回旋横置查看 f119 → 弧形轨迹回旋收回 f121-185 → 归位 f202）
"""
import struct, shutil, os, math

REF = r'reference\SlashBladeInspect\src\main\resources\assets\slashblade_inspect\combostate\inspect_a.vmd'
DEPLOY = r'src\main\resources\assets\slashbladeresh_slashblad\combostate'

def parse_bone_frames(data):
    """返回 {bone_name: {frame: (pos_off, rot_off, pos, rot)}}，帧长 111"""
    off = 30 + 20
    n = struct.unpack('<I', data[off:off+4])[0]; off += 4
    out = {}
    for _ in range(n):
        name = data[off:off+15].split(b'\x00')[0].decode('shift_jis', 'replace'); off += 15
        f = struct.unpack('<I', data[off:off+4])[0]; off += 4
        pos_off = off
        pos = struct.unpack('<3f', data[off:off+12]); off += 12
        rot_off = off
        rot = struct.unpack('<4f', data[off:off+16]); off += 16
        off += 64
        out.setdefault(name, {})[f] = (pos_off, rot_off, pos, rot)
    return out

def quat_dot(a, b):
    return sum(x*y for x, y in zip(a, b))

def quat_slerp(q1, q2, t):
    d = quat_dot(q1, q2)
    if d < 0:
        q2 = tuple(-x for x in q2); d = -d
    if d > 0.9995:
        return tuple(q1[i] + t*(q2[i]-q1[i]) for i in range(4))
    theta = math.acos(min(1.0, d))
    s = math.sin(theta)
    return tuple((math.sin((1-t)*theta)/s)*q1[i] + (math.sin(t*theta)/s)*q2[i] for i in range(4))

def build_yamato(src, deploy_path):
    data = bytearray(src)
    frames = parse_bone_frames(bytes(data))
    A = frames['hardpointA']
    q_v = (0.0, 0.0, -0.70710677, 0.70710677)
    # 鞘内姿态（f78，刀在鞘中整体横置时）
    r_sheath = A[78][3]
    # 竖立渐变：f79 0.5, f82 0.85, f83+ 完全竖立
    plan = {
        79: quat_slerp(r_sheath, q_v, 0.50),
        82: quat_slerp(r_sheath, q_v, 0.85),
        83: q_v,
        84: q_v,
        85: q_v,
        96: q_v,
    }
    for f, rot in plan.items():
        assert f in A, f'missing frame {f}'
        _, rot_off, _, _ = A[f]
        struct.pack_into('<4f', data, rot_off, *rot)
        print(f'  yamato A f{f} rot -> ({rot[0]:.3f},{rot[1]:.3f},{rot[2]:.3f},{rot[3]:.3f})')
    # 竖拔段位置：刀竖直必须居中画面内（参考横扫低位会整刀出画面）
    # 骨原点相机空间 -> VMD pos: px=x/0.15, py=(y+1.5)/0.125, pz=(0.5-z)/0.125
    pos_plan = {
        83: (-1.67, 14.0, -8.0),    # bone(-0.25, 0.25, 1.50)
        84: (0.0, 14.8, -8.4),      # bone( 0.00, 0.35, 1.55)
        85: (0.0, 14.8, -8.4),
        96: (0.0, 14.8, -8.4),
    }
    for f, pos in pos_plan.items():
        assert f in A, f'missing frame {f}'
        pos_off, _, _, _ = A[f]
        struct.pack_into('<3f', data, pos_off, *pos)
        print(f'  yamato A f{f} pos -> {pos}')
    open(deploy_path, 'wb').write(bytes(data))
    print('deployed', deploy_path, len(data), 'bytes')

os.makedirs(DEPLOY, exist_ok=True)

# 1. 通用 = 参考原样（完整 47 帧）
src = open(REF, 'rb').read()
open(os.path.join(DEPLOY, 'inspect_a.vmd'), 'wb').write(src)
print('deployed inspect_a.vmd', len(src), 'bytes (完整动画，参考原样)')

# 2. 阎魔 = 参考副本 + 拔刀段竖拔
build_yamato(src, os.path.join(DEPLOY, 'inspect_yamato.vmd'))

# 校验
for tag, path in [('inspect_a', os.path.join(DEPLOY, 'inspect_a.vmd')),
                  ('inspect_yamato', os.path.join(DEPLOY, 'inspect_yamato.vmd'))]:
    fr = parse_bone_frames(open(path, 'rb').read())
    na = len(fr.get('hardpointA', {}))
    nb = len(fr.get('hardpointB', {}))
    print(f'校验 {tag}: hardpointA {na} 帧, hardpointB {nb} 帧')
