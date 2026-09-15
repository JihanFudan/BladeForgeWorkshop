# -*- coding: utf-8 -*-
"""探查 inspect.blend 结构：对象/骨架/骨骼/动画"""
import bpy, sys

path = r'C:\Users\JihanFudan\Desktop\mod\blender_preview\inspect.blend'
bpy.ops.wm.open_mainfile(filepath=path)
print('=== 场景对象 ===')
for obj in bpy.data.objects:
    kind = obj.type
    extra = ''
    if obj.type == 'ARMATURE':
        extra = f' | bones: {[b.name for b in obj.data.bones]}'
    if obj.type == 'MESH':
        extra = f' | verts: {len(obj.data.vertices)}'
    print(f'  {obj.name} [{kind}]{extra}')
print()
print('=== 动作/动画数据 ===')
for act in bpy.data.actions:
    print(f'  action: {act.name}  frames={act.frame_range if act.frame_range else None}')
    for track in getattr(act, 'fcurves', []):
        if track.data_path and 'hardpoint' in str(track.data_path):
            print(f'    fcurve: {track.data_path}.{track.array_index}  keyframes={len(track.keyframe_points)}')
print()
print('=== 场景帧范围 ===')
print('  scene.frame_start/end:', bpy.context.scene.frame_start, bpy.context.scene.frame_end)
print('  scene.frame_current:', bpy.context.scene.frame_current)
print()
# 相机
for obj in bpy.data.objects:
    if obj.type == 'CAMERA':
        print(f'  camera {obj.name} at {obj.location} rot {obj.rotation_euler}')
