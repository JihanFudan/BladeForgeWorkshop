package cn.blockforge.generated.slashbladereshslashblad.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

/**
 * 组装动画的部件网格：读取 {@code assets/<modid>/assembly/blade_parts.json}
 * （由 animation_tools/build_parts_model.py 从拔刀剑本体 jar 的 blade.obj
 * 切分生成，四部件共享同一坐标系，落位后天然严丝合缝）。
 *
 * <p>顶点存的是"整刀已组装"位姿下的坐标（格），原点在刀镡中心、刀尖朝 -X、
 * 屏幕上为 +Y、朝摄像机为 +Z；动画骨骼位姿只是对整块网格做平移/滚转，
 * 所以 AssemblyMotion 采样的位姿回零时部件必然对齐。</p>
 *
 * <p>贴图直接复用拔刀剑本体的材质表：木偶 wood.png → sb_wood.png、
 * 竹光 bamboo.png → sb_bamboo.png（同一 UV 布局，换皮即可）。</p>
 */
public final class AssemblyStageModel {
    public static final ResourceLocation TEX_WOOD =
            ResourceLocation.fromNamespaceAndPath(cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.MOD_ID,
                    "textures/assembly/sb_wood.png");
    public static final ResourceLocation TEX_BAMBOO =
            ResourceLocation.fromNamespaceAndPath(cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.MOD_ID,
                    "textures/assembly/sb_bamboo.png");
    public static final ResourceLocation TEX_FROST =
            ResourceLocation.fromNamespaceAndPath(cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.MOD_ID,
                    "textures/assembly/sb_frost.png");

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("BladeAssemblyModel");
    private static final ResourceLocation JSON_LOC = ResourceLocation.fromNamespaceAndPath(
            cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.MOD_ID, "assembly/blade_parts.json");
    private static final ResourceLocation FROST_JSON_LOC = ResourceLocation.fromNamespaceAndPath(
            cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.MOD_ID, "assembly/frost_parts.json");

    /** 每部件一个浮点数组：[x,y,z,u,v] * 3 顶点/三角。 */
    private static volatile Map<String, float[]> parts;
    private static volatile Map<String, float[]> frostParts;
    private static final Map<ResourceLocation, RenderType> RENDER_TYPES = new HashMap<>();

    private AssemblyStageModel() {
    }

    public static void reload() {
        parts = null;
        frostParts = null;
        RENDER_TYPES.clear();
    }

    public static boolean available() {
        return load() != null;
    }

    public static boolean frostAvailable() {
        return loadFrost() != null;
    }

    /** 画一个部件；tint 为 RGB 0..1 乘色（金刀镡用暖金色，普通传白色）。 */
    public static void render(String part, PoseStack pose, MultiBufferSource buffers,
                              ResourceLocation sheet, float tintR, float tintG, float tintB, int light) {
        renderFrom(load(), part, pose, buffers, sheet, tintR, tintG, tintB, light);
    }

    /** 名刀·寒霜的组装部件：寒霜刀身 + 付丧刀鞘网格与专用材质表。 */
    public static void renderFrost(String part, PoseStack pose, MultiBufferSource buffers,
                                   float tintR, float tintG, float tintB, int light) {
        renderFrom(loadFrost(), part, pose, buffers, TEX_FROST, tintR, tintG, tintB, light);
    }

    private static void renderFrom(Map<String, float[]> map, String part, PoseStack pose,
                                   MultiBufferSource buffers, ResourceLocation sheet,
                                   float tintR, float tintG, float tintB, int light) {
        if (map == null) return;
        float[] data = map.get(part);
        if (data == null || data.length < 15) return;
        VertexConsumer vc = buffers.getBuffer(RENDER_TYPES.computeIfAbsent(sheet, RenderType::entityCutoutNoCull));
        PoseStack.Pose last = pose.last();
        // entityCutoutNoCull 的顶点缓冲是 QUADS 模式：GL 索引表每 4 个顶点切一个面，
        // 拆成三角形 (0,1,2) 和 (2,3,0)。网格按"3 顶点/三角形"直接喂入的话，
        // 第 4 个顶点会取到"下一个三角形"的首顶点——(2,3,0) 就成了横跨模型的
        // 细长垃圾面，部件之间还会互相连线，这就是"模型被拉伸"的真凶。
        // 修法：每个三角形补发一个与第 3 顶点重合的退化顶点凑满 4 个，
        // (2,3,0)=(C,C,A) 面积为零，不可见、不拉丝，部件完整刚性平移。
        for (int i = 0; i + 14 < data.length; i += 15) {
            for (int v = 0; v < 4; v++) {
                int o = i + Math.min(v, 2) * 5;
                vc.addVertex(last, data[o], data[o + 1], data[o + 2])
                        .setUv(data[o + 3], data[o + 4])
                        .setColor(tintR, tintG, tintB, 1.0f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(last, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    private static Map<String, float[]> load() {
        Map<String, float[]> local = parts;
        if (local != null) return local.isEmpty() ? null : local;
        local = read(JSON_LOC);
        parts = local == null ? new HashMap<>() : local;
        return parts;
    }

    private static Map<String, float[]> loadFrost() {
        Map<String, float[]> local = frostParts;
        if (local != null) return local.isEmpty() ? null : local;
        local = read(FROST_JSON_LOC);
        frostParts = local == null ? new HashMap<>() : local;
        return frostParts;
    }

    private static Map<String, float[]> read(ResourceLocation loc) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc)) {
            JsonObject root = JsonParser.parseString(new String(in.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject json = root.getAsJsonObject("parts");
            Map<String, float[]> out = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> e : json.entrySet()) {
                JsonArray arr = e.getValue().getAsJsonArray();
                float[] data = new float[arr.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = arr.get(i).getAsFloat();
                }
                out.put(e.getKey(), data);
            }
            LOGGER.debug("组装部件网格已加载: {} 个部件", out.size());
            return out;
        } catch (Exception e) {
            LOGGER.error("读取组装部件网格失败: {}", loc, e);
            return null;
        }
    }
}
