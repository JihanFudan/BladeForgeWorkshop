# -*- coding: utf-8 -*-
"""把指定 VMD 挂到 inspect.blend 场景，生成可直接编辑的改装配 .blend
用法: blender --background --python make_edit_blend.py -- <vmd路径> <输出blend路径> <标签>
"""
import bpy, os, sys

idx = sys.argv.index('--') if '--' in sys.argv else -1
if idx < 0 or len(sys.argv) < idx + 4:
    print('usage: -- <vmd> <out.blend> <label>')
    sys.exit(1)
VMD = sys.argv[idx + 1]
OUT = sys.argv[idx + 2]
LABEL = sys.argv[idx + 3]

SRC = r'C:\Users\JihanFudan\Desktop\mod\blender_preview\inspect.blend'

bpy.ops.wm.open_mainfile(filepath=SRC)
arm = bpy.data.objects['Bladeholder_arm']
arm.animation_data_clear()

# 隐藏人物骨架，只保留刀/鞘
alex = bpy.data.objects.get('Alex_arm')
for o in [arm]:
    o.hide_viewport = False
for mname in ['刀', '鞘']:
    mo = bpy.data.objects.get(mname)
    if mo: mo.hide_viewport = False
if alex:
    alex.hide_viewport = True

arm.select_set(True)
for o in bpy.data.objects:
    if o != arm:
        o.select_set(False)
bpy.context.view_layer.objects.active = arm

# 关键：mmd_tools 以当前帧为动画起点，先归零
bpy.context.scene.frame_set(0)

bpy.ops.mmd_tools.import_vmd(filepath=VMD, scale=0.08, create_new_action=True,
                             use_nla=False, log_level='ERROR')
sc = bpy.context.scene
sc.frame_start = 0
sc.frame_end = 202

# 验证关键帧（读 action fcurve，不依赖帧刷新）
act = arm.animation_data.action if arm.animation_data else None
print(f'[{LABEL}] action={act.name if act else None}')
if act:
    loc = {}
    for fc in act.fcurves:
        parts = fc.data_path.split('"')
        if len(parts) >= 2 and parts[1] == 'hardpointA' and parts[-1].startswith('location'):
            axis = int(parts[-1][-1])
            for kp in fc.keyframe_points:
                f = int(kp.co[0])
                loc.setdefault(f, [None, None, None])[axis] = round(kp.co[1], 4)
    frames = sorted(loc)
    for f in [0, 78, 83, 96, 119, 145, 185, 202]:
        if f in loc:
            print(f'   f{f}: {loc[f]}')

bpy.ops.wm.save_as_mainfile(filepath=OUT)
print('DONE', LABEL)
