# -*- coding: utf-8 -*-
"""
preview2.py — 按 SlashBlade Resharped 1.21.1 源码逐行重建的完整渲染链预览。
用于验证/预览 VMD 在游戏第一人称中的实际屏幕位置。

渲染链（已从源码确认，行主序后乘，列向量）：
BladeFirstPersonRender.render:
  M = XP(-clamp(xrot,-60,10)) * YP(180-yaw) * T(0,0,-0.5) * ZP(180) * S(1.2,1,1)
LayerMainBlade.render (检视):
  M *= T(0,1.5,0) * S(0.125) * ZP(180) * [Sx(-1) * skinningMat * Sx(-1)] * S(0.0625)
PmdBone: 骨原点 = VMD pos p (mmd 单位), 骨旋转 = VMD quat R (R*v + p)

推导（相机空间，z 正 = 相机前方）：
  骨原点 = (0.15px, -1.5+0.125py, 0.125pz-0.5)   [yaw=0 pitch=0]
  刀尖方向(模型+X) = (1.2*ux, uy, -uz), u = R*(-1,0,0)
完整链含 yaw/pitch 见代码。
"""
import struct
import math
from PIL import Image, ImageDraw

BLADE_LEN = 297.12 * 0.0078125  # 2.321
HANDLE_LEN = 70.71 * 0.0078125  # 0.552

def read_keys(path):
    """读取 VMD 骨骼关键帧: {bone: [(frame, pos(x,y,z), rot(x,y,z,w)), ...]}
    标准 VMD 骨骼帧 = 15(名) + 4(帧号) + 12(pos) + 16(rot) + 64(插值) = 111 字节"""
    data = open(path, 'rb').read()
    off = 30 + 20
    n = struct.unpack('<I', data[off:off+4])[0]; off += 4
    frames = {}
    for _ in range(n):
        name = data[off:off+15].split(b'\x00')[0].decode('shift_jis', 'replace'); off += 15
        f = struct.unpack('<I', data[off:off+4])[0]; off += 4
        pos = struct.unpack('<3f', data[off:off+12]); off += 12
        rot = struct.unpack('<4f', data[off:off+16]); off += 16
        off += 64  # 插值曲线 64 字节
        frames.setdefault(name, []).append((f, pos, rot))
    return frames

def quat_to_mat(q):
    x, y, z, w = q
    x2, y2, z2 = 2*x*x, 2*y*y, 2*z*z
    xy, yz, zx = 2*x*y, 2*y*z, 2*z*x
    xw, yw, zw = 2*x*w, 2*y*w, 2*z*w
    return [[1-y2-z2, xy+zw, zx-yw],
            [xy-zw, 1-z2-x2, yz+xw],
            [zx+yw, yz-xw, 1-x2-y2]]

def mat_vec3(m, v):
    return (m[0][0]*v[0]+m[0][1]*v[1]+m[0][2]*v[2],
            m[1][0]*v[0]+m[1][1]*v[1]+m[1][2]*v[2],
            m[2][0]*v[0]+m[2][1]*v[1]+m[2][2]*v[2])

def rot_y(deg, v):
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return (v[0]*c + v[2]*s, v[1], -v[0]*s + v[2]*c)

def rot_x(deg, v):
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return (v[0], v[1]*c - v[2]*s, v[1]*s + v[2]*c)

def cam_bone(p, yaw=0.0, pitch=0.0):
    """VMD pos -> 相机空间骨原点（z 正 = 前方）"""
    v = (0.125*p[0], 1.5-0.125*p[1], 0.125*p[2])
    v = (1.2*v[0], v[1], v[2])          # S(1.2,1,1)
    v = (-v[0], -v[1], v[2])            # ZP(180)
    v = (v[0], v[1], v[2]-0.5)          # T(0,0,-0.5)
    v = rot_y(180.0-yaw, v)             # YP(180-yaw)
    v = rot_x(-max(-60.0, min(10.0, pitch)), v)  # XP(-clamp)
    return v

def cam_tip(q, yaw=0.0, pitch=0.0):
    """VMD rot -> 相机空间刀尖方向(模型+X 单位向量)
    链: Sx R(q) Sx ZP(180) S(1.2) T(0,0,-0.5) YP(180-yaw) XP(-clamp)
    ZP180*Sx*R*Sx*d = (ux, -uy, uz), u = R*Sx*d = R*(-1,0,0)"""
    R = quat_to_mat(q)
    u = mat_vec3(R, (-1.0, 0.0, 0.0))   # R*Sx*d, d=(1,0,0)
    v = (u[0], -u[1], u[2])             # ZP180*Sx*R*Sx*d
    v = (1.2*v[0], v[1], v[2])          # S(1.2)
    v = rot_y(180.0-yaw, v)             # YP(180-yaw)
    v = rot_x(-max(-60.0, min(10.0, pitch)), v)  # XP(-clamp)
    return v

def project(e):
    """相机空间 -> NDC（单位焦距，y 上为正）；z 正 = 前方"""
    d = e[2]
    if d < 0.03:
        return None
    return (e[0]/d, e[1]/d)

def sample(vmd_path, frame, bone='hardpointA'):
    """线性采样关键帧"""
    keys = read_keys(vmd_path).get(bone, [])
    if not keys:
        return None
    keys = sorted(keys)
    if frame <= keys[0][0]:
        return keys[0][1], keys[0][2]
    for i in range(len(keys)-1):
        f0, p0, r0 = keys[i]
        f1, p1, r1 = keys[i+1]
        if f0 <= frame <= f1:
            t = 0 if f1 == f0 else (frame-f0)/(f1-f0)
            p = tuple(a+(b-a)*t for a, b in zip(p0, p1))
            r = tuple(a+(b-a)*t for a, b in zip(r0, r1))
            return p, r
    return keys[-1][1], keys[-1][2]

def render_frame(vmd_path, frame, size=(400, 400), yaw=0.0, pitch=0.0,
                 draw_handle=True, label=True):
    img = Image.new('RGB', size, (28, 32, 40))
    dr = ImageDraw.Draw(img)
    W, H = size
    cx, cy = W//2, H//2
    scale = H*0.42  # NDC ±1 -> 像素
    for i in range(-2, 3):
        dr.line([(cx+i*scale, 0), (cx+i*scale, H)], fill=(50, 55, 66), width=1)
        dr.line([(0, cy+i*scale), (W, cy+i*scale)], fill=(50, 55, 66), width=1)
    dr.line([(0, cy), (W, cy)], fill=(90, 95, 110), width=2)
    dr.line([(cx, 0), (cx, H)], fill=(90, 95, 110), width=2)

    data = read_keys(vmd_path)
    for bone, color, w in [('hardpointA', (240, 240, 245), 5),
                           ('hardpointB', (120, 130, 150), 4)]:
        if bone not in data:
            continue
        p, r = sample(vmd_path, frame, bone)
        ea = cam_bone(p, yaw, pitch)
        sa = project(ea)
        if sa is None:
            continue
        tip_dir = cam_tip(r, yaw, pitch)
        et = (ea[0]+BLADE_LEN*tip_dir[0], ea[1]+BLADE_LEN*tip_dir[1], ea[2]+BLADE_LEN*tip_dir[2])
        st = project(et)
        if st is None:
            continue
        eb = (ea[0]-HANDLE_LEN*tip_dir[0], ea[1]-HANDLE_LEN*tip_dir[1], ea[2]-HANDLE_LEN*tip_dir[2])
        sb = project(eb)
        pts = [(cx+s[0]*scale, cy-s[1]*scale) for s in (sb, sa, st)]
        if all(0 <= x <= W and 0 <= y <= H for x, y in pts):
            dr.line([pts[0], pts[1], pts[2]], fill=color, width=w)
        else:
            dr.line([pts[0], pts[1], pts[2]], fill=color, width=w)
        dr.ellipse([pts[1][0]-6, pts[1][1]-6, pts[1][0]+6, pts[1][1]+6], fill=(255, 200, 60))
        # 输出刀尖/骨原点位置
        print(f'  {bone} f{frame}: bone=({ea[0]:.2f},{ea[1]:.2f},{ea[2]:.2f}) depth={ea[2]:.2f} '
              f'screen=({sa[0]:+.2f},{sa[1]:+.2f}) tip_dir=({tip_dir[0]:+.2f},{tip_dir[1]:+.2f},{tip_dir[2]:+.2f})')
    if label:
        dr.text((8, 6), f'frame {frame}', fill=(200, 210, 230))
    return img

def contact_sheet(vmd_path, frames, out_png, cols=5, size=(360, 360), yaw=0.0, pitch=0.0):
    rows = (len(frames)+cols-1)//cols
    sheet = Image.new('RGB', (cols*size[0], rows*size[1]), (15, 17, 22))
    for i, fr in enumerate(frames):
        cell = render_frame(vmd_path, fr, size=size, yaw=yaw, pitch=pitch)
        sheet.paste(cell, ((i % cols)*size[0], (i//cols)*size[1]))
    sheet.save(out_png)
    print('saved', out_png, sheet.size)
    return sheet

if __name__ == '__main__':
    ref = r'reference\SlashBladeInspect\src\main\resources\assets\slashblade_inspect\combostate\inspect_a.vmd'
    contact_sheet(ref, [0, 20, 40, 60, 80, 100, 119, 140, 160, 185], r'animation_tools\ref_a_check.png', cols=5)
