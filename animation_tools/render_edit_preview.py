# -*- coding: utf-8 -*-
"""渲染改装配 yamato_edit.blend 关键帧预览（确认用户打开后看到的效果）"""
import bpy, sys, os

idx = sys.argv.index('--') if '--' in sys.argv else -1
BLEND = sys.argv[idx + 1]
OUT_DIR = sys.argv[idx + 2]
FRAMES = [int(x) for x in sys.argv[idx + 3].split(',')]

bpy.ops.wm.open_mainfile(filepath=BLEND)
sc = bpy.context.scene

# 设置渲染引擎 Eevee / 工作台着色
sc.render.engine = 'BLENDER_EEVEE'
sc.render.resolution_x = 640
sc.render.resolution_y = 640
sc.render.film_transparent = True
sc.render.image_settings.file_format = 'PNG'

# 添加相机对准刀（Bladeholder 在世界原点附近）
cam_data = bpy.data.cameras.new('preview_cam')
cam = bpy.data.objects.new('preview_cam', cam_data)
sc.collection.objects.link(cam)
cam.location = (0.0, -2.6, 0.6)
cam.rotation_euler = (1.50, 0.0, 0.0)  # 俯视玩家前方
sc.camera = cam

# 世界背景
if sc.world is None:
    w = bpy.data.worlds.new('preview_world')
    sc.world = w
sc.world.use_nodes = True
bg = sc.world.node_tree.nodes.get('Background')
if bg:
    bg.inputs[0].default_value = (0.12, 0.13, 0.16, 1.0)

for f in FRAMES:
    sc.frame_set(f)
    bpy.context.view_layer.update()
    out = os.path.join(OUT_DIR, f'preview_f{f}.png')
    sc.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print('rendered', out)
print('DONE')
