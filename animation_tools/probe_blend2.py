# -*- coding: utf-8 -*-
import bpy
bpy.ops.wm.open_mainfile(filepath=r'C:\Users\JihanFudan\Desktop\mod\blender_preview\inspect.blend')
print('=== 场景对象 ===')
for obj in bpy.data.objects:
    extra = ''
    if obj.type == 'ARMATURE':
        extra = ' | bones: ' + ', '.join(b.name for b in obj.data.bones)
    if obj.type == 'MESH':
        extra = f' | verts={len(obj.data.vertices)}'
    print('  %s [%s]%s' % (obj.name, obj.type, extra))
print()
print('=== 硬点骨骼 rest pose ===')
for obj in bpy.data.objects:
    if obj.type == 'ARMATURE':
        print('  Armature: %s' % obj.name)
        for pb in obj.pose.bones:
            if 'hardpoint' in pb.name.lower() or 'point' in pb.name.lower():
                print('    bone %s: head=%s tail=%s' % (pb.name, tuple(round(v,3) for v in pb.head), tuple(round(v,3) for v in pb.tail)))
        for child in obj.children:
            print('    child: %s [%s]' % (child.name, child.type))
print()
print('=== 相机 ===')
for obj in bpy.data.objects:
    if obj.type == 'CAMERA':
        print('  %s: loc=%s rot=%s' % (obj.name, tuple(round(v,3) for v in obj.location), tuple(round(v,3) for v in obj.rotation_euler)))
print()
print('=== 动作绑定 ===')
for obj in bpy.data.objects:
    if obj.animation_data and obj.animation_data.action:
        print('  %s -> action: %s' % (obj.name, obj.animation_data.action.name))
