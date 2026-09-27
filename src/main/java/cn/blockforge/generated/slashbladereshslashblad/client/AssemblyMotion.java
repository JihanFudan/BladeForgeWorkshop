package cn.blockforge.generated.slashbladereshslashblad.client;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 组装动画的 VMD 播放器：读取
 * {@code assets/<modid>/assembly/blade_assembly.vmd}（由
 * animation_tools/assembly_vmd.py 生成；Blender 端改 blender_preview/blade_assembly.glb
 * 的动画后，用 animation_tools/export_assembly_vmd.py 写回同格式）。
 *
 * <p>VMD 用 MMD 标准约定：X 右、Y 下、Z 朝屏幕外，8 单位 = 1 格，
 * 绕 Z 滚转正值为屏幕顺时针。本类采样后换算回舞台坐标：
 * x 右为正、y 上为正、z 朝摄像机为正、roll 屏幕逆时针为正。</p>
 *
 * <p>四根骨骼：blade_blank 刀条 / tsuba 刀镡 / handle 刀柄 / sheath 刀鞘。
 * 部件网格（{@link AssemblyStageModel}）在生成时已按"整刀横放、刀尖朝左"
 * 拼进同一坐标系，所以每根骨骼的落座位都是 (0,0,0)：动画只描述
 * "离位飞入 → 回零落座"的偏移，位姿回零四部件即严丝合缝。
 * 时间轴：30fps，0-120 帧 = 80 游戏刻（1 刻 = 1.5 帧），与服务端
 * BladeAssembly 的三段落座刻（28/50/66）对齐。</p>
 */
public final class AssemblyMotion {
    public static final float FRAMES_PER_TICK = 1.5f;
    public static final int TOTAL_FRAMES = 120;
    /** VMD 单位 / 格。 */
    public static final float UNITS_PER_BLOCK = 8.0f;

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("BladeAssemblyMotion");

    /** 舞台位姿：x 右、y 上、z 朝摄像机（单位：格），roll 屏幕逆时针（度）。 */
    public record Pose(float x, float y, float z, float roll) {
        public static final Pose HIDDEN = new Pose(0, 100, 0, 0);
    }

    private static final class Track {
        int[] frames = new int[0];
        float[] xs = new float[0];
        float[] ys = new float[0];
        float[] zs = new float[0];
        float[] rolls = new float[0];
    }

    private static volatile Map<String, Track> tracks;

    private AssemblyMotion() {
    }

    /** 丢弃缓存，下次采样时重新读取（每次组装开始都会调用，方便换包调试）。 */
    public static void reload() {
        tracks = null;
    }

    public static boolean available() {
        return tracks() != null;
    }

    /** 采样某骨骼在给定帧的位姿；动画数据缺失时返回 null。 */
    public static Pose sample(String bone, float frame) {
        Map<String, Track> map = tracks();
        if (map == null) return null;
        Track t = map.get(bone);
        if (t == null || t.frames.length == 0) return null;
        float f = Mth.clamp(frame, t.frames[0], t.frames[t.frames.length - 1]);
        int lo = 0;
        while (lo + 1 < t.frames.length && t.frames[lo + 1] <= f) lo++;
        int hi = Math.min(lo + 1, t.frames.length - 1);
        if (lo == hi) {
            return new Pose(t.xs[lo], t.ys[lo], t.zs[lo], t.rolls[lo]);
        }
        float k = (f - t.frames[lo]) / (t.frames[hi] - t.frames[lo]);
        return new Pose(
                Mth.lerp(k, t.xs[lo], t.xs[hi]),
                Mth.lerp(k, t.ys[lo], t.ys[hi]),
                Mth.lerp(k, t.zs[lo], t.zs[hi]),
                Mth.lerp(k, t.rolls[lo], t.rolls[hi]));
    }

    private static Map<String, Track> tracks() {
        Map<String, Track> local = tracks;
        if (local != null) return local.isEmpty() ? null : local;
        local = load();
        tracks = local == null ? new HashMap<>() : local;
        return local;
    }

    private static Map<String, Track> load() {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "assembly/blade_assembly.vmd");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc)) {
            byte[] data = in.readAllBytes();
            if (data.length < 54 || !startsWith(data, "Vocaloid Motion Data 0002")) {
                LOGGER.error("组装动画 VMD 头不合法: {}", loc);
                return null;
            }
            Charset sjis = Charset.forName("Shift_JIS");
            int count = (int) readU32(data, 50);
            Map<String, TreeMap<Integer, float[]>> raw = new HashMap<>();
            int off = 54;
            for (int i = 0; i < count && off + 111 <= data.length; i++, off += 111) {
                String name = new String(data, off, 15, sjis).trim();
                int end = name.indexOf('\u0000');
                if (end >= 0) name = name.substring(0, end);
                int frame = (int) readU32(data, off + 15);
                float px = readF(data, off + 19);
                float py = readF(data, off + 23);
                float pz = readF(data, off + 27);
                float qx = readF(data, off + 31);
                float qy = readF(data, off + 35);
                float qz = readF(data, off + 39);
                float qw = readF(data, off + 43);
                // MMD 约定 -> 舞台约定：x 不变、y 取反、z(朝屏幕外)不变、roll 取反
                float roll = (float) (-2.0 * Math.atan2(qz, qw) * 180.0 / Math.PI);
                raw.computeIfAbsent(name, k -> new TreeMap<>())
                        .put(frame, new float[]{px / UNITS_PER_BLOCK, -py / UNITS_PER_BLOCK,
                                pz / UNITS_PER_BLOCK, roll});
            }
            Map<String, Track> out = new HashMap<>();
            for (Map.Entry<String, TreeMap<Integer, float[]>> e : raw.entrySet()) {
                Track t = new Track();
                t.frames = new int[e.getValue().size()];
                t.xs = new float[t.frames.length];
                t.ys = new float[t.frames.length];
                t.zs = new float[t.frames.length];
                t.rolls = new float[t.frames.length];
                int i = 0;
                for (Map.Entry<Integer, float[]> k : e.getValue().entrySet()) {
                    t.frames[i] = k.getKey();
                    t.xs[i] = k.getValue()[0];
                    t.ys[i] = k.getValue()[1];
                    t.zs[i] = k.getValue()[2];
                    t.rolls[i] = k.getValue()[3];
                    i++;
                }
                out.put(e.getKey(), t);
            }
            LOGGER.debug("组装动画 VMD 已加载: {} 骨骼 {} 根", loc, out.size());
            return out;
        } catch (Exception e) {
            LOGGER.error("读取组装动画 VMD 失败: {}", loc, e);
            return null;
        }
    }

    private static boolean startsWith(byte[] data, String text) {
        byte[] t = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (data.length < t.length) return false;
        for (int i = 0; i < t.length; i++) {
            if (data[i] != t[i]) return false;
        }
        return true;
    }

    private static long readU32(byte[] d, int off) {
        return (d[off] & 0xFFL) | (d[off + 1] & 0xFFL) << 8 | (d[off + 2] & 0xFFL) << 16 | (d[off + 3] & 0xFFL) << 24;
    }

    private static float readF(byte[] d, int off) {
        return Float.intBitsToFloat((d[off] & 0xFF) | (d[off + 1] & 0xFF) << 8
                | (d[off + 2] & 0xFF) << 16 | (d[off + 3] & 0xFF) << 24);
    }
}
