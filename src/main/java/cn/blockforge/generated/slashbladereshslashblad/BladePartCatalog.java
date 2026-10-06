package cn.blockforge.generated.slashbladereshslashblad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** 扫描前置拔刀剑的动态注册表，把结构正常的整刀模型作为可选部件来源。 */
public final class BladePartCatalog {
    /* 刀刃是主要判定依据；范围按拔刀剑本体模型放宽，容许正常的长度、弧度与轻微装饰差异。 */
    private static final float MIN_BLADE_LENGTH = 240.0F;
    private static final float MAX_BLADE_LENGTH = 345.0F;
    private static final float MAX_BLADE_WIDTH = 72.0F;
    private static final float MAX_BLADE_THICKNESS = 28.0F;
    /* 刀根装饰会放大全组边界，因此另取 X<=-60 的真正刀刃主体做二次检查。 */
    private static final float BLADE_CORE_END_X = -60.0F;
    private static final float MAX_BLADE_CORE_WIDTH = 40.0F;
    private static final float MAX_BLADE_CORE_THICKNESS = 8.0F;
    /* 沿刀长分段测量刀身中线：正常日式刀允许平顺弧度，但拒绝急折、反复蛇形或过度弯曲。 */
    private static final int BLADE_CURVE_SLICES = 12;
    /* 至少三个实际几何点才构成有效截面；寒霜资源会去重顶点，而不是在这里放宽判定。 */
    private static final int MIN_CURVE_SLICE_VERTICES = 3;
    private static final float MAX_BLADE_CENTERLINE_SPAN = 28.0F;
    private static final float MAX_BLADE_LOCAL_SLOPE = 0.30F;
    private static final int MAX_BLADE_CURVE_REVERSALS = 1;
    private static final float CURVE_DIRECTION_EPSILON = 0.035F;
    private static final float MIN_BLADE_TIP_X = -360.0F;
    private static final float MAX_BLADE_TIP_X = -230.0F;
    private static final float MIN_BLADE_ROOT_X = -5.0F;
    private static final float MAX_BLADE_ROOT_X = 65.0F;

    /* 没有独立 handle 分组时，从标准 blade 分组的刀根右侧识别刀柄。 */
    private static final float HANDLE_START_X = -32.8F;
    private static final float MIN_HANDLE_LENGTH = 28.0F;
    private static final float MAX_HANDLE_LENGTH = 115.0F;
    private static final float MAX_HANDLE_WIDTH = 52.0F;
    private static final float MAX_HANDLE_THICKNESS = 32.0F;

    /* 刀鞘只检查远离鞘口的鞘头一半，鞘口一半的挂饰不会再造成误判。 */
    private static final float MIN_SHEATH_TIP_HALF_LENGTH = 90.0F;
    private static final float MAX_SHEATH_TIP_HALF_LENGTH = 165.0F;
    private static final float MAX_SHEATH_TIP_HALF_WIDTH = 65.0F;
    private static final float MAX_SHEATH_TIP_HALF_THICKNESS = 30.0F;

    /* 刀镡允许完全不存在；存在时也必须保持在正常拔刀剑刀镡的小型范围内。 */
    private static final float MAX_GUARD_LENGTH = 45.0F;
    private static final float MAX_GUARD_WIDTH = 80.0F;
    private static final float MAX_GUARD_THICKNESS = 55.0F;

    private static final Map<ResourceLocation, Boolean> MODEL_COMPATIBILITY = new ConcurrentHashMap<>();

    /** 几何资源无法读取前的快速兜底；实际决定结果的仍是 OBJ 整刀检测。 */
    private static final Set<String> KNOWN_EXTREME_MODEL_MARKERS = Set.of(
            "/tboen/", "/wanderer/", "amazingshining", "amazing_shining", "fluorescent");

    public record Entry(ResourceLocation definition, ResourceLocation model, ResourceLocation texture,
                        String translationKey) {
        public String shortName() {
            return definition.getNamespace() + ":" + definition.getPath();
        }
    }

    private BladePartCatalog() {}

    public static List<Entry> scan(net.minecraft.core.RegistryAccess access) {
        Registry<SlashBladeDefinition> registry = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
        List<Entry> result = new ArrayList<>();
        for (ResourceLocation key : registry.keySet()) {
            SlashBladeDefinition definition = registry.get(key);
            if (definition == null || key.getPath().equals("none") || isBrokenDefinition(definition)) continue;
            ResourceLocation model = definition.getRenderDefinition().getModelName();
            if (!isCompatibleModel(model)) continue;
            result.add(new Entry(key, model, definition.getRenderDefinition().getTextureName(),
                    definition.getTranslationKey()));
        }
        /* 木偶、竹光、利刀白鞘是物品注册项，不在 named_blades 动态注册表中，需显式补入。
         * 三者共用已经过几何检查的标准 blade.obj，并非绕过模型检测的白名单。 */
        addBaseBlade(result, "slashblade_wood", "wood");
        addBaseBlade(result, "slashblade_bamboo", "bamboo");
        addBaseBlade(result, "slashblade_white", "white");
        result.sort(Comparator.comparing(e -> e.definition().toString()));
        return List.copyOf(result);
    }

    private static void addBaseBlade(List<Entry> result, String id, String texture) {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath("slashblade", "model/blade.obj");
        if (!isCompatibleModel(model)) return;
        ResourceLocation definition = ResourceLocation.fromNamespaceAndPath("slashblade", id);
        if (result.stream().anyMatch(entry -> entry.definition().equals(definition))) return;
        result.add(new Entry(definition, model,
                ResourceLocation.fromNamespaceAndPath("slashblade", "model/" + texture + ".png"),
                "item.slashblade." + id));
    }

    /** 断刃和刀刃碎片状态直接拒绝，不能因与完整刀共用同一个 OBJ 而混入列表。 */
    private static boolean isBrokenDefinition(SlashBladeDefinition definition) {
        List<SwordType> types = definition.getStateDefinition().getDefaultType();
        return types.contains(SwordType.BROKEN) || types.contains(SwordType.EDGEFRAGMENT);
    }

    private static boolean isCompatibleModel(ResourceLocation model) {
        if (model == null) return false;
        String id = (model.getNamespace() + ":/" + model.getPath()).toLowerCase(Locale.ROOT).replace('\\', '/');
        for (String marker : KNOWN_EXTREME_MODEL_MARKERS) {
            if (id.contains(marker)) return false;
        }
        return MODEL_COMPATIBILITY.computeIfAbsent(model, BladePartCatalog::inspectWholeBladeGeometry);
    }

    /**
     * 读取 OBJ 后同时检查刀刃、刀柄、可选刀镡及可选刀鞘。模型读不到时不再放行，因为无法证明
     * 它具备自定义渲染器需要的刀刃和刀柄；正常的无鞘刀、无镡刀仍可通过。
     */
    private static boolean inspectWholeBladeGeometry(ResourceLocation model) {
        String resourcePath = "assets/" + model.getNamespace() + "/" + model.getPath();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = BladePartCatalog.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resourcePath)) {
            return stream != null && inspectObj(stream);
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean inspectObj(InputStream stream) throws IOException {
        List<Vertex> vertices = new ArrayList<>();
        PartVertices parts = new PartVertices();
        Part current = Part.OTHER;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.startsWith("v ")) {
                    String[] values = line.split("\\s+");
                    if (values.length >= 4) {
                        try {
                            vertices.add(new Vertex(Float.parseFloat(values[1]), Float.parseFloat(values[2]),
                                    Float.parseFloat(values[3])));
                        } catch (NumberFormatException ignored) {
                            return false;
                        }
                    }
                } else if (line.startsWith("g ") || line.startsWith("o ")) {
                    current = classify(line.substring(2).strip().toLowerCase(Locale.ROOT));
                } else if (current != Part.OTHER && line.startsWith("f ")) {
                    Set<Integer> face = parseFace(line, vertices.size());
                    if (face == null) return false;
                    parts.add(current, face);
                }
            }
        }

        if (parts.blade.isEmpty()) return false;
        Bounds blade = Bounds.of(vertices, parts.blade);
        if (!validBlade(blade) || !validBladeCore(vertices, parts.blade)
                || !validBladeCurvature(vertices, parts.blade)) return false;

        Set<Integer> handleVertices = new HashSet<>(parts.handle);
        if (handleVertices.isEmpty()) {
            for (int index : parts.blade) {
                if (vertices.get(index).x() >= HANDLE_START_X) handleVertices.add(index);
            }
        }
        if (handleVertices.isEmpty() || !validHandle(Bounds.of(vertices, handleVertices))) return false;

        if (!parts.guard.isEmpty() && !validGuard(Bounds.of(vertices, parts.guard))) return false;
        return parts.sheath.isEmpty() || validSheathTipHalf(vertices, parts.sheath);
    }

    private static Set<Integer> parseFace(String line, int vertexCount) {
        Set<Integer> result = new HashSet<>();
        for (String token : line.substring(2).strip().split("\\s+")) {
            String indexText = token.split("/", -1)[0];
            try {
                int raw = Integer.parseInt(indexText);
                int index = raw > 0 ? raw - 1 : vertexCount + raw;
                if (index < 0 || index >= vertexCount) return null;
                result.add(index);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private static boolean validBlade(Bounds bounds) {
        float length = bounds.lengthX();
        return length >= MIN_BLADE_LENGTH && length <= MAX_BLADE_LENGTH
                && bounds.widthY() <= MAX_BLADE_WIDTH && bounds.widthZ() <= MAX_BLADE_THICKNESS
                && bounds.minX() >= MIN_BLADE_TIP_X && bounds.minX() <= MAX_BLADE_TIP_X
                && bounds.maxX() >= MIN_BLADE_ROOT_X && bounds.maxX() <= MAX_BLADE_ROOT_X;
    }

    /**
     * 只看刀尖到刀身中后段，避开正常刀镡和刀根装饰。枯石大刀的主体在这一段已经宽到约 47，
     * 而原版标准刀与各把正常名刀为 21～34；此检查因此能按模型本身排除大幅异形刀刃。
     */
    private static boolean validBladeCore(List<Vertex> vertices, Set<Integer> blade) {
        Set<Integer> core = new HashSet<>();
        for (int index : blade) {
            if (vertices.get(index).x() <= BLADE_CORE_END_X) core.add(index);
        }
        if (core.isEmpty()) return false;
        Bounds bounds = Bounds.of(vertices, core);
        return bounds.lengthX() >= MIN_BLADE_LENGTH * 0.65F
                && bounds.widthY() <= MAX_BLADE_CORE_WIDTH
                && bounds.widthZ() <= MAX_BLADE_CORE_THICKNESS;
    }

    private static boolean validBladeCurvature(List<Vertex> vertices, Set<Integer> blade) {
        List<Vertex> core = new ArrayList<>();
        for (int index : blade) {
            Vertex vertex = vertices.get(index);
            if (vertex.x() <= BLADE_CORE_END_X) core.add(vertex);
        }
        if (core.size() < 6) return false;

        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        for (Vertex vertex : core) {
            minX = Math.min(minX, vertex.x());
            maxX = Math.max(maxX, vertex.x());
        }
        float length = maxX - minX;
        if (length <= 0.0F) return false;

        List<CurvePoint> points = new ArrayList<>();
        for (int slice = 0; slice < BLADE_CURVE_SLICES; slice++) {
            float from = minX + length * slice / BLADE_CURVE_SLICES;
            float to = minX + length * (slice + 1) / BLADE_CURVE_SLICES;
            List<Float> sliceY = new ArrayList<>();
            for (Vertex vertex : core) {
                boolean inside = vertex.x() >= from
                        && (slice == BLADE_CURVE_SLICES - 1 ? vertex.x() <= to : vertex.x() < to);
                if (inside) sliceY.add(vertex.y());
            }
            /* 三个以上几何点并取中位数，避免单个刀纹点扭曲中线，同时保持严格的小范围弧度检测。 */
            if (sliceY.size() >= MIN_CURVE_SLICE_VERTICES) {
                sliceY.sort(Float::compare);
                int middle = sliceY.size() / 2;
                float medianY = sliceY.size() % 2 == 0
                        ? (sliceY.get(middle - 1) + sliceY.get(middle)) * 0.5F : sliceY.get(middle);
                points.add(new CurvePoint((from + to) * 0.5F, medianY));
            }
        }
        if (points.size() < 4) return false;

        float minCenterY = Float.POSITIVE_INFINITY, maxCenterY = Float.NEGATIVE_INFINITY;
        int reversals = 0;
        int direction = 0;
        for (int i = 0; i < points.size(); i++) {
            CurvePoint point = points.get(i);
            minCenterY = Math.min(minCenterY, point.y());
            maxCenterY = Math.max(maxCenterY, point.y());
            if (i == 0) continue;
            CurvePoint previous = points.get(i - 1);
            float dx = point.x() - previous.x();
            if (dx <= 0.0F) return false;
            float slope = (point.y() - previous.y()) / dx;
            if (Math.abs(slope) > MAX_BLADE_LOCAL_SLOPE) return false;
            int nextDirection = slope > CURVE_DIRECTION_EPSILON ? 1
                    : slope < -CURVE_DIRECTION_EPSILON ? -1 : 0;
            if (nextDirection != 0) {
                if (direction != 0 && direction != nextDirection) reversals++;
                direction = nextDirection;
            }
        }
        return maxCenterY - minCenterY <= MAX_BLADE_CENTERLINE_SPAN
                && reversals <= MAX_BLADE_CURVE_REVERSALS;
    }

    private static boolean validHandle(Bounds bounds) {
        return bounds.lengthX() >= MIN_HANDLE_LENGTH && bounds.lengthX() <= MAX_HANDLE_LENGTH
                && bounds.widthY() <= MAX_HANDLE_WIDTH && bounds.widthZ() <= MAX_HANDLE_THICKNESS
                && bounds.maxX() >= 0.0F && bounds.maxX() <= 145.0F;
    }

    private static boolean validGuard(Bounds bounds) {
        return bounds.lengthX() <= MAX_GUARD_LENGTH && bounds.widthY() <= MAX_GUARD_WIDTH
                && bounds.widthZ() <= MAX_GUARD_THICKNESS;
    }

    private static boolean validSheathTipHalf(List<Vertex> vertices, Set<Integer> sheath) {
        Bounds full = Bounds.of(vertices, sheath);
        float middleX = (full.minX() + full.maxX()) * 0.5F;
        Set<Integer> tipHalf = new HashSet<>();
        for (int index : sheath) {
            if (vertices.get(index).x() <= middleX) tipHalf.add(index);
        }
        if (tipHalf.isEmpty()) return false;
        Bounds tip = Bounds.of(vertices, tipHalf);
        return tip.lengthX() >= MIN_SHEATH_TIP_HALF_LENGTH && tip.lengthX() <= MAX_SHEATH_TIP_HALF_LENGTH
                && tip.widthY() <= MAX_SHEATH_TIP_HALF_WIDTH
                && tip.widthZ() <= MAX_SHEATH_TIP_HALF_THICKNESS;
    }

    private static Part classify(String name) {
        boolean luminous = name.contains("luminous") || name.contains("lumino");
        String semantic = name.replace("_luminous", "").replace("_lumino", "");
        if (luminous || semantic.startsWith("item_")) return Part.OTHER;
        if (semantic.contains("damaged") || semantic.contains("fragment") || semantic.contains("broken")) {
            return Part.BROKEN;
        }
        if (semantic.equals("blade") || semantic.equals("katana") || semantic.equals("sword")
                || semantic.startsWith("blade_")) return Part.BLADE;
        if (semantic.equals("handle") || semantic.equals("hilt") || semantic.equals("tsuka")
                || semantic.equals("grip") || semantic.startsWith("handle_") || semantic.startsWith("hilt_")) {
            return Part.HANDLE;
        }
        if (semantic.equals("guard") || semantic.equals("tsuba") || semantic.equals("handguard")
                || semantic.startsWith("guard_") || semantic.startsWith("tsuba_")) return Part.GUARD;
        if (semantic.equals("sheath") || semantic.equals("saya") || semantic.equals("scabbard")
                || semantic.startsWith("sheath_") || semantic.startsWith("saya_")) return Part.SHEATH;
        return Part.OTHER;
    }

    private enum Part { BLADE, HANDLE, GUARD, SHEATH, BROKEN, OTHER }

    private static final class PartVertices {
        private final Set<Integer> blade = new HashSet<>();
        private final Set<Integer> handle = new HashSet<>();
        private final Set<Integer> guard = new HashSet<>();
        private final Set<Integer> sheath = new HashSet<>();

        private void add(Part part, Set<Integer> face) {
            switch (part) {
                case BLADE -> blade.addAll(face);
                case HANDLE -> handle.addAll(face);
                case GUARD -> guard.addAll(face);
                case SHEATH -> sheath.addAll(face);
                default -> { }
            }
        }
    }

    private record Vertex(float x, float y, float z) {}
    private record CurvePoint(float x, float y) {}

    private record Bounds(float minX, float maxX, float widthY, float widthZ) {
        private float lengthX() { return maxX - minX; }

        private static Bounds of(List<Vertex> vertices, Set<Integer> indices) {
            float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (int index : indices) {
                Vertex vertex = vertices.get(index);
                minX = Math.min(minX, vertex.x());
                maxX = Math.max(maxX, vertex.x());
                minY = Math.min(minY, vertex.y());
                maxY = Math.max(maxY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxZ = Math.max(maxZ, vertex.z());
            }
            return new Bounds(minX, maxX, maxY - minY, maxZ - minZ);
        }
    }

    public static Entry selected(net.minecraft.core.RegistryAccess access, int index) {
        List<Entry> entries = scan(access);
        if (entries.isEmpty()) return null;
        return entries.get(Math.clamp(index, 0, entries.size() - 1));
    }
}
