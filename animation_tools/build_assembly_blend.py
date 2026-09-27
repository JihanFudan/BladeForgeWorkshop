# -*- coding: utf-8 -*-
"""
build_assembly_blend.py —— 在 Blender 里把 blade_assembly.vmd 烘焙成
「刀条/刀镡/刀柄/刀鞘」四部件的组装动画 .blend，并可顺带渲一组预览帧。

用法（容器内已验证 Blender 4.2 LTS 无头可跑）:
  blender --background --python animation_tools/build_assembly_blend.py -- \
      <vmd路径> <输出blend路径> [渲染输出目录]

坐标映射（与 assembly_vmd.py / AssemblyMotion.java 同一套约定）：
  VMD(MMD): X右, Y下, Z朝屏幕外, 8单位=1格, 绕Z正=屏幕顺时针
  Blender:  loc=(x, -z, -y)/8 ;  rotation_euler=(0, φ, 0), φ=2*atan2(qz,qw)
"""
import math
import os
import struct
import sys

TOTAL_FRAMES = 120
U = 8.0

# 名字: (显示名, 贴图, Blender尺寸 X宽/Y厚/Z长, 单位=格)
# 宽度按预览可读性放大到能看清 16x16 物品贴图；游戏内仍用物品图标渲染。
PARTS = {
    "blade_blank": ("刀条", "wooden_blade_blank.png", (0.160, 0.030, 0.450)),
    "tsuba":       ("刀镡", "tsuba_wood_iron.png",    (0.200, 0.030, 0.200)),
    "handle":      ("刀柄", "blade_handle.png",       (0.120, 0.050, 0.300)),
    "sheath":      ("刀鞘", "blade_sheath.png",       (0.160, 0.060, 0.550)),
}
TEX_DIR = "src/main/resources/assets/slashbladeresh_slashblad/textures/item"


def read_vmd(path):
    """返回 {bone: {frame: (pos3, quat4)}}，VMD 为 MMD 标准单位。"""
    data = open(path, "rb").read()
    assert data[:25] == b"Vocaloid Motion Data 0002"
    n = struct.unpack("<I", data[50:54])[0]
    off = 54
    out = {}
    for _ in range(n):
        name = data[off:off + 15].split(b"\x00")[0].decode("shift_jis"); off += 15
        f = struct.unpack("<I", data[off:off + 4])[0]; off += 4
        pos = struct.unpack("<3f", data[off:off + 12]); off += 12
        rot = struct.unpack("<4f", data[off:off + 16]); off += 16
        off += 64
        out.setdefault(name, {})[f] = (pos, rot)
    return out


def main():
    idx = sys.argv.index("--") if "--" in sys.argv else -1
    args = sys.argv[idx + 1:] if idx >= 0 else []
    vmd_path = args[0] if len(args) > 0 else "src/main/resources/assets/slashbladeresh_slashblad/assembly/blade_assembly.vmd"
    blend_path = args[1] if len(args) > 1 else "blender_preview/assembly_edit.blend"
    render_dir = args[2] if len(args) > 2 else None
    print("ARGS", args)

    root = os.path.dirname(os.path.abspath(blend_path))
    proj = os.path.dirname(root) if os.path.basename(root) == "blender_preview" else root
    import bpy
    from mathutils import Vector

    tracks = read_vmd(os.path.join(proj, vmd_path) if not os.path.isabs(vmd_path) else vmd_path)

    # ---- 干净场景 ----
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.fps = 30
    scene.frame_start = 0
    scene.frame_end = TOTAL_FRAMES

    # ---- 部件网格 + 材质 ----
    objects = {}
    for bone, (cname, tex, size) in PARTS.items():
        mesh = bpy.data.meshes.new(cname)
        hx, hy, hz = (v / 2 for v in size)
        verts = [(-hx, -hy, -hz), (hx, -hy, -hz), (hx, hy, -hz), (-hx, hy, -hz),
                 (-hx, -hy, hz), (hx, -hy, hz), (hx, hy, hz), (-hx, hy, hz)]
        faces = [(4, 5, 1, 0), (7, 3, 2, 6), (5, 6, 2, 1), (0, 3, 7, 4),
                 (1, 2, 3, 0), (7, 6, 5, 4)]
        mesh.from_pydata([Vector(v) for v in verts], [], faces)
        mesh.update()
        # UV：正/背面（法线 ±Y）铺满整张贴图，侧面采样贴图中心避免透明边
        uv_layer = mesh.uv_layers.new(name="UVMap")
        for poly in mesh.polygons:
            front = abs(poly.normal.y) > 0.5
            for li in poly.loop_indices:
                v = verts[mesh.loops[li].vertex_index]
                if front:
                    uv_layer.data[li].uv = (v[0] / (2 * hx) + 0.5, v[2] / (2 * hz) + 0.5)
                else:
                    uv_layer.data[li].uv = (0.5, 0.5)
        mat = bpy.data.materials.new(cname)
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes["Principled BSDF"]
        img = bpy.data.images.load(os.path.join(proj, TEX_DIR, tex))
        tex_node = mat.node_tree.nodes.new("ShaderNodeTexImage")
        tex_node.image = img
        mat.node_tree.links.new(tex_node.outputs["Color"], bsdf.inputs["Base Color"])
        try:
            mat.node_tree.links.new(tex_node.outputs["Alpha"], bsdf.inputs["Alpha"])
        except TypeError:
            pass
        mat.blend_method = "BLEND"
        obj = bpy.data.objects.new(cname, mesh)
        obj.data.materials.append(mat)
        scene.collection.objects.link(obj)
        objects[bone] = obj

    # ---- 烘焙 VMD 关键帧 ----
    for bone, keys in tracks.items():
        obj = objects.get(bone)
        if obj is None:
            print("!! VMD 骨骼", bone, "无对应部件，跳过")
            continue
        frames = sorted(keys)
        for f in frames:
            pos, quat = keys[f]
            x, y, z = (v / U for v in pos)
            roll = 2.0 * math.atan2(quat[2], quat[3])  # MMD 绕Z 正=屏幕顺时针
            scene.frame_set(f)
            obj.location = (x, -z, -y)
            obj.rotation_euler = (0.0, roll, 0.0)
            obj.keyframe_insert("location", frame=f)
            obj.keyframe_insert("rotation_euler", frame=f)
        for fc in obj.animation_data.action.fcurves:
            for kp in fc.keyframe_points:
                kp.interpolation = "LINEAR"

    # ---- 摄像机 / 灯光 / 世界 ----
    cam_data = bpy.data.cameras.new("Cam")
    cam_data.lens = 30
    cam = bpy.data.objects.new("Cam", cam_data)
    cam.location = (0.0, -3.9, 0.20)
    cam.rotation_euler = (math.radians(90), 0, 0)
    scene.collection.objects.link(cam)
    scene.camera = cam

    light_data = bpy.data.lights.new("Sun", "SUN")
    light_data.energy = 3.0
    light = bpy.data.objects.new("Sun", light_data)
    light.rotation_euler = (math.radians(50), math.radians(-15), math.radians(20))
    scene.collection.objects.link(light)

    world = bpy.data.worlds.new("World")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs[0].default_value = (0.12, 0.12, 0.14, 1)
    scene.world = world

    # ---- 约定说明写进 blend 文本块 ----
    txt = bpy.data.texts.new("组装动画约定.txt")
    txt.write(
        "四部件组装动画（与游戏内 blade_assembly.vmd 同源）\n"
        "帧率 30fps，0-120 帧 = 80 游戏刻；\n"
        "刀镡落座 f42(刻28) / 刀柄落座 f75(刻50) / 入鞘落座 f99(刻66)。\n"
        "坐标：Blender X=屏幕右, Z=屏幕上, -Y=朝屏幕外；1 Blender 单位 = 1 格。\n"
        "改完关键帧后运行 export_assembly_vmd.py 导出回 .vmd，替换资源即可。")

    os.makedirs(os.path.dirname(blend_path) or ".", exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=blend_path)
    print("BLEND SAVED ->", blend_path)

    # ---- 渲染预览帧（Cycles CPU：无头容器没有 OpenGL，Workbench/EEVEE 跑不了） ----
    if render_dir:
        os.makedirs(render_dir, exist_ok=True)
        scene.render.engine = "CYCLES"
        scene.cycles.device = "CPU"
        scene.cycles.samples = 24
        try:
            scene.cycles.use_denoising = False
        except AttributeError:
            pass
        scene.render.resolution_x = 480
        scene.render.resolution_y = 270
        scene.render.image_settings.file_format = "PNG"
        for f in (0, 8, 20, 42, 55, 75, 84, 90, 96, 99, 110, 120):
            scene.frame_set(f)
            scene.render.filepath = os.path.join(render_dir, f"f{f:03d}.png")
            bpy.ops.render.render(write_still=True)
        print("RENDERED ->", render_dir)


main()
