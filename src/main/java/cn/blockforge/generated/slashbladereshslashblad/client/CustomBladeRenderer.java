package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.BladePartCatalog;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mods.flammpfeil.slashblade.client.renderer.SlashBladeTEISR;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * 完整复用拔刀剑本体的 {@link SlashBladeTEISR}。本类不再自行判断第一/第三人称，
 * 因此持刀、收刀、腰间刀鞘、物品栏、掉落物与展示框均沿用前置模组的原版链路。
 * 真正的四部件替换在 {@link #onRenderOverride(RenderOverrideEvent)} 中完成。
 */
public final class CustomBladeRenderer extends SlashBladeTEISR {
    private static final ResourceLocation DEFAULT_MODEL = ResourceLocation.fromNamespaceAndPath("slashblade", "model/blade.obj");
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath("slashblade", "model/white.png");
    private static final int WHITE = FastColor.ARGB32.color(255, 255, 255, 255);
    private static final float DEFAULT_BLADE_END_X = -42.4841F;
    /** 刀根钢芯继续伸入刀镡内部；只接纳贴近刀身中轴的窄面，不把横向刀镡或装饰带进刀刃。 */
    private static final float BLADE_ROOT_INSERT_MAX_X = -34.0F;
    private static final float BLADE_ROOT_MAX_ABS_Z = 1.5F;
    private static final float BLADE_ROOT_MIN_Y = -3.0F;
    private static final float BLADE_ROOT_MAX_Y = 9.5F;
    /** 与组装动画拆件完全相同的刀镡范围，不能只按 X 相交，否则会把断刀长度的刀条长面带进来。 */
    private static final float ASSEMBLY_GUARD_MIN_X = -45.0F;
    private static final float ASSEMBLY_GUARD_MAX_X = -15.0F;
    private static final float ASSEMBLY_GUARD_MIN_RADIAL_Z = 4.0F;
    private static final float ASSEMBLY_GUARD_MIN_Y = -7.0F;
    private static final float ASSEMBLY_GUARD_MAX_Y = 14.0F;
    private static final float DEFAULT_HANDLE_START_X = -32.7929F;
    /** 刀柄材质向刀镡内延伸一小段，填平接缝；范围只覆盖中轴附近的标准柄首截面。 */
    private static final float HANDLE_SEAM_OVERLAP_X = 3.5F;
    private static final float HANDLE_SEAM_MAX_ABS_Z = 3.75F;
    private static final float HANDLE_SEAM_MIN_Y = -3.75F;
    private static final float HANDLE_SEAM_MAX_Y = 10.0F;

    public CustomBladeRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    /**
     * BladeRenderState 在所有原版场景最终都会发出此事件。我们保留事件自带的 PoseStack，
     * 只取消原来的整刀网格，并在完全相同的位置绘制玩家选择的四个来源部件。
     */
    public static void onRenderOverride(RenderOverrideEvent event) {
        ItemStack stack = event.getStack();
        if (!stack.is(GeneratedMod.CUSTOM_BLADE.get())) return;

        String target = event.getTarget().toLowerCase(Locale.ROOT);
        boolean luminous = target.endsWith("_luminous") || target.endsWith("_lumino");
        String baseTarget = luminous
                ? target.substring(0, target.lastIndexOf('_'))
                : target;

        boolean replaced = switch (baseTarget) {
            case "blade", "blade_damaged", "blade_fragment" -> {
                renderWorldBlade(stack, luminous, event);
                yield true;
            }
            case "sheath" -> {
                renderPart(stack, 2, PartKind.SHEATH, luminous, event.getPoseStack(), event);
                yield true;
            }
            case "item_blade", "item_damaged", "item_bladens" -> {
                renderItemIcon(stack, !baseTarget.equals("item_bladens"), luminous, event);
                yield true;
            }
            default -> false;
        };
        if (replaced) event.setCanceled(true);
    }

    private static void renderWorldBlade(ItemStack stack, boolean luminous, RenderOverrideEvent event) {
        PoseStack pose = event.getPoseStack();
        renderPart(stack, 0, PartKind.BLADE, luminous, pose, event);
        renderPart(stack, 1, PartKind.GUARD, luminous, pose, event);
        renderPart(stack, 3, PartKind.HANDLE, luminous, pose, event);
    }

    /** item_blade 是原版专用于物品栏的刀/鞘交叉构图；这里复用其标准矩阵。 */
    private static void renderItemIcon(ItemStack stack, boolean withSheath, boolean luminous,
                                       RenderOverrideEvent event) {
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(defaultItemBladeTransform());
        renderWorldBlade(stack, luminous, event);
        pose.popPose();

        if (withSheath) {
            pose.pushPose();
            pose.mulPose(defaultItemSheathTransform());
            renderPart(stack, 2, PartKind.SHEATH, luminous, pose, event);
            pose.popPose();
        }
    }

    /** 在制作台侧栏绘制当前下拉栏选中的真实 OBJ 部件，所见即最终组装后的来源模型。 */
    public static void renderWorkbenchPreview(GuiGraphics graphics, BladePartCatalog.Entry entry, int part,
                                               int centerX, int centerY, int width, int height) {
        if (entry == null || part < 0 || part > 3) return;
        WavefrontObject model = BladeModelManager.getInstance().getModel(entry.model());
        PartKind kind = PartKind.values()[part];
        /* 侧栏要展示来源模型本身，而不是模拟游戏内普通/发光两遍渲染。把两层合并后，
         * 发光组中独有的刀尖、鞘尾和装饰面也会出现在预览里。 */
        List<Face> faces = new ArrayList<>(collect(model, kind, false));
        faces.addAll(collect(model, kind, true));
        if (faces.isEmpty()) return;
        boolean guardFront = kind == PartKind.GUARD;
        PreviewBounds bounds = PreviewBounds.of(faces, guardFront);
        if (!bounds.valid()) return;

        float scale = Math.min(Math.max(1.0F, width - 8.0F) / Math.max(1.0F, bounds.width()),
                Math.max(1.0F, height - 8.0F) / Math.max(1.0F, bounds.height()));
        scale = Math.min(scale, 2.4F);

        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 180.0F);
        pose.scale(scale, -scale, scale);
        if (guardFront) {
            /* OBJ 中刀长沿 X，刀镡正面位于 Y-Z 平面；绕 Y 轴转 90° 后玩家看到的是盘面。 */
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.translate(-bounds.depthCenter(), -bounds.centerY(), -bounds.centerX());
        } else {
            pose.translate(-bounds.centerX(), -bounds.centerY(), -bounds.depthCenter());
        }
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(entry.texture()));
        for (Face face : faces) face.addFaceForRender(vertices, pose, LightTexture.FULL_BRIGHT, WHITE);
        graphics.flush();
        pose.popPose();
    }

    private static void renderPart(ItemStack stack, int index, PartKind kind, boolean luminous,
                                   PoseStack pose, RenderOverrideEvent event) {
        ResourceLocation modelId = BladeData.customPartResource(stack, index, "Model", DEFAULT_MODEL);
        ResourceLocation texture = BladeData.customPartResource(stack, index, "Texture", DEFAULT_TEXTURE);
        WavefrontObject model = BladeModelManager.getInstance().getModel(modelId);
        List<Face> faces = collect(model, kind, luminous);
        if (faces.isEmpty()) return;

        RenderType renderType = event.getGetRenderType().apply(texture);
        VertexConsumer vertices = event.getBuffer().getBuffer(renderType);
        for (Face face : faces) face.addFaceForRender(vertices, pose, event.getPackedLightIn(), WHITE);

        if (!luminous && stack.hasFoil() && event.isEnableEffect()) {
            RenderType glint = event.getTarget().startsWith("item_")
                    ? BladeRenderState.getSlashBladeItemGlint()
                    : BladeRenderState.getSlashBladeGlint();
            VertexConsumer glintVertices = event.getBuffer().getBuffer(glint);
            for (Face face : faces) face.addFaceForRender(glintVertices, pose, event.getPackedLightIn(), WHITE);
        }
    }

    /**
     * 发光组与普通组必须分开收集；旧实现把 blade_luminous 同时塞进普通刀身，
     * 正是部分模型重叠、发花的来源之一。
     */
    private static List<Face> collect(WavefrontObject model, PartKind kind, boolean luminous) {
        List<Face> blade = new ArrayList<>();
        List<Face> handle = new ArrayList<>();
        List<Face> guard = new ArrayList<>();
        List<Face> sheath = new ArrayList<>();
        for (GroupObject group : model.groupObjects) {
            String name = group.name.toLowerCase(Locale.ROOT);
            boolean groupLuminous = name.contains("luminous") || name.contains("lumino");
            if (groupLuminous != luminous || isIconOrDamageGroup(name)) continue;
            String semantic = name.replace("_luminous", "").replace("_lumino", "");
            if (isBladeGroup(semantic)) blade.addAll(group.faces);
            if (isHandleGroup(semantic)) handle.addAll(group.faces);
            if (isGuardGroup(semantic)) guard.addAll(group.faces);
            if (isSheathGroup(semantic)) sheath.addAll(group.faces);
        }
        List<Face> embeddedGuard = selectEmbeddedGuard(blade);
        return switch (kind) {
            case SHEATH -> sheath;
            case BLADE -> selectBladeOnly(blade);
            case GUARD -> !guard.isEmpty() ? withMissingGuardHub(guard, blade)
                    : !embeddedGuard.isEmpty() ? embeddedGuard : selectEmbeddedGuard(handle);
            case HANDLE -> !handle.isEmpty() ? selectNamedHandle(handle) : selectEmbeddedHandle(blade);
        };
    }

    /**
     * 保留刀身以及跨过刀刃终点的收口长面，但裁掉完全位于刀根右侧的旧套环。
     * 不能按 maxX 硬切，否则标准刀身最后一段长面会消失，物品栏里刀刃与刀柄之间出现断口。
     */
    private static List<Face> selectBladeOnly(List<Face> faces) {
        if (faces.isEmpty()) return List.of();
        List<Face> result = new ArrayList<>();
        for (Face face : faces) {
            Bounds bounds = Bounds.of(face);
            if (bounds.minX() < DEFAULT_BLADE_END_X - 0.75F || isBladeRootInsert(bounds)) result.add(face);
        }
        return result.isEmpty() ? faces : result;
    }

    private static boolean isBladeRootInsert(Bounds bounds) {
        return bounds.minX() >= DEFAULT_BLADE_END_X - 0.1F
                && bounds.maxX() <= BLADE_ROOT_INSERT_MAX_X
                && bounds.maxAbsZ() <= BLADE_ROOT_MAX_ABS_Z
                && bounds.minY() >= BLADE_ROOT_MIN_Y
                && bounds.maxY() <= BLADE_ROOT_MAX_Y;
    }

    /**
     * 独立 guard 分组有时只包含刀镡外圈，刀簇（刀身穿过刀镡的中心套口）仍留在 blade 分组。
     * 只补入刀镡薄层内、贴近中轴且不属于刀刃钢芯的面，既补全寒霜刀簇，也不会拿回旧刀柄残片。
     */
    private static List<Face> withMissingGuardHub(List<Face> guard, List<Face> blade) {
        List<Face> result = new ArrayList<>(guard);
        for (Face face : blade) {
            Bounds bounds = Bounds.of(face);
            boolean inGuardLayer = bounds.minX() >= -38.1F && bounds.maxX() <= -35.1F;
            boolean centralHub = bounds.maxAbsZ() <= ASSEMBLY_GUARD_MIN_RADIAL_Z
                    && bounds.minY() >= ASSEMBLY_GUARD_MIN_Y
                    && bounds.maxY() <= ASSEMBLY_GUARD_MAX_Y;
            if (inGuardLayer && centralHub && !isBladeRootInsert(bounds)) result.add(face);
        }
        return result;
    }

    /**
     * 按组装动画的刀镡判定拆面：面中心位于刀根区，同时必须横向离开刀身中轴。
     * 旧逻辑只检查 X 相交，刀刃从 -110 延伸到刀根的长三角面也会命中，结果正好多出
     * 一截 blade_damaged（断刀）长度的刀条。这里的 -45..-15 与 |Z|>=4 参数直接对齐
     * {@code build_parts_model.py} 的组装动画拆件边界，只拿刀镡盘本身，不裁入刀条。
     */
    private static List<Face> selectEmbeddedGuard(List<Face> faces) {
        if (faces.isEmpty()) return List.of();
        List<Face> result = new ArrayList<>();
        for (Face face : faces) {
            Bounds b = Bounds.of(face);
            if (isAssemblyGuardFace(b)) {
                result.add(face);
            }
        }
        return result;
    }

    /**
     * blade 分组中没有独立 handle 时，在柄首平面上真正切开跨界面，只保留刀柄一侧。
     * 洞爷湖没有刀镡遮挡接缝，旧逻辑只要面的一个顶点进入刀柄区就保留整张面，因此跨区长面会把
     * 一大截刀条一起绘制成刀柄。直接丢弃跨区面又会让正常刀柄缺角；平面裁剪可以同时保住柄首
     * 和连接处的小幅重叠，并彻底去掉分界线左侧的刀条部分，无需针对具体刀型写白名单。
     */
    private static List<Face> selectEmbeddedHandle(List<Face> faces) {
        if (faces.isEmpty()) return List.of();
        float cutX = DEFAULT_HANDLE_START_X - HANDLE_SEAM_OVERLAP_X;
        List<Face> result = new ArrayList<>();
        for (Face face : faces) {
            if (isAssemblyGuardFace(Bounds.of(face))) continue;
            result.addAll(clipFaceToHandle(face, cutX));
        }
        return result;
    }

    /** 用 Sutherland-Hodgman 算法把 OBJ 多边形裁到 {@code x >= cutX}，并保留插值后的贴图与法线。 */
    private static List<Face> clipFaceToHandle(Face face, float cutX) {
        List<ClippedVertex> input = new ArrayList<>();
        for (int i = 0; i < face.vertices.length; i++) input.add(ClippedVertex.of(face, i));
        List<ClippedVertex> polygon = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ClippedVertex previous = input.get((i + input.size() - 1) % input.size());
            ClippedVertex current = input.get(i);
            boolean previousInside = previous.position().x >= cutX;
            boolean currentInside = current.position().x >= cutX;
            if (previousInside != currentInside) polygon.add(ClippedVertex.atX(previous, current, cutX));
            if (currentInside) polygon.add(current);
        }
        if (polygon.size() < 3) return List.of();

        List<Face> result = new ArrayList<>();
        for (int i = 1; i < polygon.size() - 1; i++) {
            result.add(makeFace(face, polygon.get(0), polygon.get(i), polygon.get(i + 1)));
        }
        return result;
    }

    private static Face makeFace(Face source, ClippedVertex first, ClippedVertex second, ClippedVertex third) {
        ClippedVertex[] points = {first, second, third};
        Face face = new Face();
        face.vertices = new Vertex[3];
        for (int i = 0; i < points.length; i++) face.vertices[i] = points[i].position();
        if (source.textureCoordinates != null && source.textureCoordinates.length == source.vertices.length) {
            face.textureCoordinates = new TextureCoordinate[3];
            for (int i = 0; i < points.length; i++) face.textureCoordinates[i] = points[i].texture();
            face.initTexSigns();
        }
        if (source.vertexNormals != null && source.vertexNormals.length == source.vertices.length) {
            face.vertexNormals = new Vertex[3];
            for (int i = 0; i < points.length; i++) face.vertexNormals[i] = points[i].normal();
        }
        face.faceNormal = source.faceNormal;
        return face;
    }

    /**
     * 独立 handle 分组的语义已经明确，因此应优先完整保留该组，而不是再用标准刀坐标硬裁一次。
     * 只剔除少数被作者一并塞入 handle 组、且在刀镡区横向展开的面；这样异形刀柄、跨边界长面
     * 和柄尾端盖都会完整带入，不会再出现少面或从侧面看内部是空的情况。
     */
    private static List<Face> selectNamedHandle(List<Face> faces) {
        if (faces.isEmpty()) return List.of();
        List<Face> result = new ArrayList<>();
        for (Face face : faces) {
            Bounds bounds = Bounds.of(face);
            if (!isAssemblyGuardFace(bounds)) result.add(face);
        }
        return result.isEmpty() ? faces : result;
    }

    private static boolean isAssemblyGuardFace(Bounds bounds) {
        return bounds.centerX() >= ASSEMBLY_GUARD_MIN_X
                && bounds.centerX() < ASSEMBLY_GUARD_MAX_X
                /* 有些原版刀镡（例如洞爷湖）主要沿 Y 方向展开，Z 很薄，不能只看 Z。 */
                && (bounds.maxAbsZ() >= ASSEMBLY_GUARD_MIN_RADIAL_Z
                    || bounds.minY() <= ASSEMBLY_GUARD_MIN_Y
                    || bounds.maxY() >= ASSEMBLY_GUARD_MAX_Y);
    }

    private static boolean isBladeGroup(String name) {
        return name.equals("blade") || name.equals("katana") || name.equals("sword")
                || name.startsWith("blade_") && !name.contains("damaged") && !name.contains("fragment");
    }

    private static boolean isSheathGroup(String name) {
        return name.equals("sheath") || name.equals("saya") || name.equals("scabbard")
                || name.startsWith("sheath_") || name.startsWith("saya_");
    }

    private static boolean isHandleGroup(String name) {
        return name.equals("handle") || name.equals("hilt") || name.equals("tsuka") || name.equals("grip")
                || name.startsWith("handle_") || name.startsWith("hilt_");
    }

    private static boolean isGuardGroup(String name) {
        return name.equals("guard") || name.equals("tsuba") || name.equals("handguard")
                || name.startsWith("guard_") || name.startsWith("tsuba_");
    }

    private static boolean isIconOrDamageGroup(String name) {
        return name.startsWith("item_") || name.contains("damaged") || name.contains("fragment");
    }

    private static Matrix4f defaultItemBladeTransform() {
        return new Matrix4f()
                .m00(-0.3513422091F).m10(-1.2292924936F).m20(0.0000086237F).m30(-63.2631436785F)
                .m01(0.4696877503F).m11(-0.8699876138F).m21(-0.0000101678F).m31(57.8811116816F)
                .m02(0.0F).m12(0.0F).m22(1.4039793118F).m32(15.593F);
    }

    private static Matrix4f defaultItemSheathTransform() {
        return new Matrix4f()
                .m00(0.3425939586F).m10(0.77083591F).m20(-0.0000083467F).m30(63.8568170358F)
                .m01(0.4477656188F).m11(-0.6248078361F).m21(-0.0000066559F).m31(62.4343619704F)
                .m02(0.0F).m12(0.0F).m22(-0.6393679184F).m32(8.4590103634F);
    }

    private enum PartKind { BLADE, GUARD, SHEATH, HANDLE }

    private record PreviewBounds(float minX, float maxX, float minY, float maxY,
                                 float minDepth, float maxDepth) {
        float width() { return maxX - minX; }
        float height() { return maxY - minY; }
        float centerX() { return (minX + maxX) * 0.5F; }
        float centerY() { return (minY + maxY) * 0.5F; }
        float depthCenter() { return (minDepth + maxDepth) * 0.5F; }
        boolean valid() { return Float.isFinite(minX) && Float.isFinite(maxX) && width() > 0.0F && height() > 0.0F; }

        static PreviewBounds of(List<Face> faces, boolean guardFront) {
            float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            float minDepth = Float.POSITIVE_INFINITY, maxDepth = Float.NEGATIVE_INFINITY;
            for (Face face : faces) for (var vertex : face.vertices) {
                float screenX = guardFront ? vertex.z : vertex.x;
                float depth = guardFront ? vertex.x : vertex.z;
                minX = Math.min(minX, screenX);
                maxX = Math.max(maxX, screenX);
                minY = Math.min(minY, vertex.y);
                maxY = Math.max(maxY, vertex.y);
                minDepth = Math.min(minDepth, depth);
                maxDepth = Math.max(maxDepth, depth);
            }
            return new PreviewBounds(minX, maxX, minY, maxY, minDepth, maxDepth);
        }
    }

    private record ClippedVertex(Vertex position, TextureCoordinate texture, Vertex normal) {
        static ClippedVertex of(Face face, int index) {
            TextureCoordinate texture = face.textureCoordinates != null && index < face.textureCoordinates.length
                    ? face.textureCoordinates[index] : null;
            Vertex normal = face.vertexNormals != null && index < face.vertexNormals.length
                    ? face.vertexNormals[index] : null;
            return new ClippedVertex(face.vertices[index], texture, normal);
        }

        static ClippedVertex atX(ClippedVertex from, ClippedVertex to, float x) {
            float span = to.position.x - from.position.x;
            float ratio = Math.abs(span) < 1.0E-6F ? 0.0F : (x - from.position.x) / span;
            Vertex position = new Vertex(x,
                    lerp(from.position.y, to.position.y, ratio),
                    lerp(from.position.z, to.position.z, ratio));
            TextureCoordinate texture = from.texture != null && to.texture != null
                    ? new TextureCoordinate(lerp(from.texture.u, to.texture.u, ratio),
                            lerp(from.texture.v, to.texture.v, ratio),
                            lerp(from.texture.w, to.texture.w, ratio)) : null;
            Vertex normal = from.normal != null && to.normal != null
                    ? normalized(lerp(from.normal.x, to.normal.x, ratio),
                            lerp(from.normal.y, to.normal.y, ratio),
                            lerp(from.normal.z, to.normal.z, ratio)) : null;
            return new ClippedVertex(position, texture, normal);
        }

        private static float lerp(float from, float to, float ratio) {
            return from + (to - from) * ratio;
        }

        private static Vertex normalized(float x, float y, float z) {
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            return length > 1.0E-6F ? new Vertex(x / length, y / length, z / length) : new Vertex(x, y, z);
        }
    }

    private record Bounds(float minX, float maxX, float centerX, float minY, float maxY, float maxAbsZ) {
        static Bounds of(Face face) {
            float minX = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float sumX = 0.0F;
            float maxAbsZ = 0.0F;
            for (var vertex : face.vertices) {
                minX = Math.min(minX, vertex.x);
                maxX = Math.max(maxX, vertex.x);
                minY = Math.min(minY, vertex.y);
                maxY = Math.max(maxY, vertex.y);
                sumX += vertex.x;
                maxAbsZ = Math.max(maxAbsZ, Math.abs(vertex.z));
            }
            return new Bounds(minX, maxX, sumX / face.vertices.length, minY, maxY, maxAbsZ);
        }
    }
}
