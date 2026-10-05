package cn.blockforge.generated.slashbladereshslashblad.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.blockforge.generated.slashbladereshslashblad.BladeAssembly;
import cn.blockforge.generated.slashbladereshslashblad.BladeAssemblyNetwork.StartPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 组装动画客户端：在玩家面前约一臂处悬浮渲染四个部件的 <b>真实 3D 网格</b>
 * （拔刀剑本体 blade.obj 切分，见 {@link AssemblyStageModel}），
 * 按 blade_assembly.vmd 的时间轴完成横放组装：
 * 刀条横着飞入 → 刀镡从刀条右侧滑来穿过茎部 → 刀柄从右侧推上罩住茎 →
 * 刀鞘从刀条左侧横着出现、向右平移罩住刀条 → 特效收束，成品刀进背包。
 *
 * <p>所有位置/朝向都来自 {@link AssemblyMotion} 对 VMD 的采样；四部件的
 * 落座位都是骨骼原点 (0,0,0)，网格顶点在生成时就在同一坐标系里拼好，
 * 所以"回零即严丝合缝"，不存在手工对位。</p>
 *
 * <p>想在 Blender 里改动作：导入 blender_preview/blade_assembly.glb
 * （模型+动画一体），改完导出同名 glb，再跑 animation_tools/export_assembly_vmd.py
 * 写回 blade_assembly.vmd。详见 blender_preview/如何改组装动画.txt。</p>
 */
public final class BladeAssemblyClient {
    private static final class Active {
        final int entityId;
        final long startGameTime;
        final int kind;
        final int metal;
        int snapStage;
        boolean finaleBurst;

        Active(StartPayload payload) {
            this.entityId = payload.entityId();
            this.startGameTime = payload.startGameTime();
            this.kind = payload.kind();
            this.metal = payload.metal();
        }
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    /** 1.21.1 的阶段事件不给 BufferSource，与 SSA 演出同款：自建即时缓冲。 */
    private static final com.mojang.blaze3d.vertex.ByteBufferBuilder STAGE_BUFFER =
            new com.mojang.blaze3d.vertex.ByteBufferBuilder(262144);
    private static final MultiBufferSource.BufferSource STAGE_BUFFERS = MultiBufferSource.immediate(STAGE_BUFFER);

    private BladeAssemblyClient() {
    }

    public static void start(StartPayload payload) {
        AssemblyMotion.reload();      // 每次组装重读 VMD，方便换资源包热调试
        AssemblyStageModel.reload();  // 部件网格同理
        ACTIVE.put(payload.entityId(), new Active(payload));
    }

    /** 该实体是否正在组装（0..DURATION 内的动画年龄，-1 表示没有）。供手臂姿势 mixin 使用。 */
    public static float animationAge(int entityId, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        Active active = ACTIVE.get(entityId);
        if (active == null) return -1;
        float age = mc.level.getGameTime() - active.startGameTime + partial;
        return age >= 0 && age <= BladeAssembly.DURATION_TICKS ? age : -1;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void renderAssembly(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return;
        long now = mc.level.getGameTime();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Iterator<Active> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Active active = iterator.next();
            float age = now - active.startGameTime + partial;
            if (age > BladeAssembly.DURATION_TICKS + 2) {
                iterator.remove();
                continue;
            }
            if (!(mc.level.getEntity(active.entityId) instanceof Player player)) continue;
            if (age >= 0) {
                spawnSnapBursts(mc, active, event, player, age, partial);
                drawStage(mc, active, event, player, age);
            }
        }
        STAGE_BUFFERS.endBatch();
    }

    /* ---------------- 渲染 ---------------- */

    /** 收尾"特效收束进背包"的起点（刻）。 */
    private static final float FINALE_START_TICK = BladeAssembly.DURATION_TICKS - 10.0f;
    /** 舞台倾角：消除正对屏幕的"平面拉丝感"（渲染与火花定位共用，见 tiltStagePose）。 */
    private static final float STAGE_TILT_X = 14.0f;
    private static final float STAGE_TILT_Y = 12.0f;

    private static void drawStage(Minecraft mc, Active active, RenderLevelStageEvent event, Player player, float age) {
        boolean frost = active.kind == BladeAssembly.KIND_FROST;
        if (!AssemblyMotion.available()) return;
        if (frost ? !AssemblyStageModel.frostAvailable() : !AssemblyStageModel.available()) return;
        Vec3 anchor = stageAnchor(player, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(anchor.x - camPos.x, anchor.y - camPos.y, anchor.z - camPos.z);
        // 面向摄像机（仅偏航）：此后 +X=右、+Y=上、-Z=朝摄像机，1 单位 = 1 格。
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - camera.getYRot()));
        // 舞台整体带一点俯仰/侧摆（同拔刀剑物品 GUI 的 display 变换思路）：
        // 完全正对屏幕时细长刀身会被看成"拉平的纸条"，倾角让刀尖/刀背带上
        // 透视纵深与厚度，拉伸感即消失。
        pose.mulPose(Axis.XP.rotationDegrees(STAGE_TILT_X));
        pose.mulPose(Axis.YP.rotationDegrees(STAGE_TILT_Y));

        float frame = age * AssemblyMotion.FRAMES_PER_TICK;
        float gather = ease(age, 0.0f, 8.0f);
        float finale = ease(age, FINALE_START_TICK, BladeAssembly.DURATION_TICKS);
        // 收尾：整刀缩小、向玩家怀中沉，模拟"收进背包"；表现层，不属于动画数据。
        float pop = 0.35f + 0.65f * gather;
        float shrink = 1.0f - finale;
        float scale = pop * shrink * shrink;
        if (finale > 0.0f) {
            pose.translate(0.0, -0.18 * finale, 0.08 * finale);
        }
        pose.scale(Math.max(scale, 0.001f), Math.max(scale, 0.001f), Math.max(scale, 0.001f));
        if (finale > 0.0f && !active.finaleBurst) {
            active.finaleBurst = true;
            spawnFinaleSparks(mc, player, anchor);
        }

        ResourceLocation sheet = active.kind == BladeAssembly.KIND_BAMBOO
                ? AssemblyStageModel.TEX_BAMBOO : AssemblyStageModel.TEX_WOOD;
        AssemblyMotion.Pose blank = AssemblyMotion.sample("blade_blank", frame);
        AssemblyMotion.Pose handle = AssemblyMotion.sample("handle", frame);
        AssemblyMotion.Pose tsuba = AssemblyMotion.sample("tsuba", frame);
        AssemblyMotion.Pose sheath = AssemblyMotion.sample("sheath", frame);

        // 名刀·寒霜用自己的混合网格与材质（寒霜刀身 + 付丧刀鞘），其余刀用木偶/竹光材质。
        if (frost) {
            drawFrostPart(pose, STAGE_BUFFERS, "blade_blank", blank);
            drawFrostPart(pose, STAGE_BUFFERS, "handle", handle);
            drawFrostPart(pose, STAGE_BUFFERS, "tsuba", tsuba);
            drawFrostPart(pose, STAGE_BUFFERS, "sheath", sheath);
            pose.popPose();
            return;
        }

        // 由远及近：刀条 → 刀柄 → 刀镡（盘最厚，扣在茎上）→ 刀鞘（最后罩住整条刀身）。
        drawPart(pose, STAGE_BUFFERS, "blade_blank", blank, sheet, 1.0f, 1.0f, 1.0f);
        drawPart(pose, STAGE_BUFFERS, "handle", handle, sheet, 1.0f, 1.0f, 1.0f);
        // 刀镡按金属染色：铁保持本色、金偏暖黄、铜偏橙红。
        float tintG = active.metal == BladeAssembly.METAL_GOLD ? 0.84f : active.metal == BladeAssembly.METAL_COPPER ? 0.60f : 1.0f;
        float tintB = active.metal == BladeAssembly.METAL_GOLD ? 0.45f : active.metal == BladeAssembly.METAL_COPPER ? 0.35f : 1.0f;
        drawPart(pose, STAGE_BUFFERS, "tsuba", tsuba, sheet, 1.0f, tintG, tintB);
        drawPart(pose, STAGE_BUFFERS, "sheath", sheath, sheet, 1.0f, 1.0f, 1.0f);

        pose.popPose();
    }

    /** 画一个部件：位置/朝向取自 VMD 采样（格）；舞台 z 朝摄像机为正，映射到 -Z。 */
    private static void drawPart(PoseStack pose, MultiBufferSource buffers, String part,
                                 AssemblyMotion.Pose p, ResourceLocation sheet,
                                 float tintR, float tintG, float tintB) {
        if (p == null) return;
        pose.pushPose();
        pose.translate(p.x(), p.y(), -p.z());
        pose.mulPose(Axis.ZP.rotationDegrees(p.roll()));
        AssemblyStageModel.render(part, pose, buffers, sheet, tintR, tintG, tintB, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** 寒霜专用：部件网格与材质都来自这把刀自己。 */
    private static void drawFrostPart(PoseStack pose, MultiBufferSource buffers, String part,
                                      AssemblyMotion.Pose p) {
        if (p == null) return;
        pose.pushPose();
        pose.translate(p.x(), p.y(), -p.z());
        pose.mulPose(Axis.ZP.rotationDegrees(p.roll()));
        AssemblyStageModel.renderFrost(part, pose, buffers, 1.0f, 1.0f, 1.0f, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** 视线前方约一臂、略低于眼睛的位置。 */
    private static Vec3 stageAnchor(Player player, float partial) {
        Vec3 look = player.getLookAngle();
        return player.getEyePosition(partial)
                .add(look.x * 0.95, look.y * 0.95 - 0.06, look.z * 0.95);
    }

    /* ---------------- 落座火花 / 收束特效 ---------------- */

    private static void spawnSnapBursts(Minecraft mc, Active active, RenderLevelStageEvent event,
                                        Player player, float age, float partial) {
        int stage = age >= BladeAssembly.SHEATHE_SNAP_TICK ? 3
                : age >= BladeAssembly.HANDLE_SNAP_TICK ? 2
                : age >= BladeAssembly.TSUBA_SNAP_TICK ? 1 : 0;
        if (stage <= active.snapStage) return;
        active.snapStage = stage;
        String bone = switch (stage) {
            case 1 -> "tsuba";
            case 2 -> "handle";
            case 3 -> "sheath";
            default -> null;
        };
        AssemblyMotion.Pose p = bone == null ? null : AssemblyMotion.sample(bone, age * AssemblyMotion.FRAMES_PER_TICK);
        // 落座时只在部件周围点几缕薄雾，不要糊满屏幕（用户反馈雾气太多）。
        spawnBurstAtPose(mc, player, partial, p, 3, 0.12);
    }

    /** 收束特效：整刀缩进怀里的同时轻轻打出一圈 END_ROD/雷花。 */
    private static void spawnFinaleSparks(Minecraft mc, Player player, Vec3 anchor) {
        spawnBurstAtPose(mc, player, 0.0f, null, 6, 0.22);
        for (int i = 0; i < 4; i++) {
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    anchor.x + (mc.level.random.nextDouble() - 0.5) * 0.5,
                    anchor.y + (mc.level.random.nextDouble() - 0.5) * 0.2,
                    anchor.z + (mc.level.random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.05, 0.0);
        }
    }

    /** 在舞台坐标 p 处（按摄像机偏航换算回世界）打一圈粒子。 */
    private static void spawnBurstAtPose(Minecraft mc, Player player, float partial,
                                         AssemblyMotion.Pose p, int count, double spread) {
        Vec3 anchor = stageAnchor(player, partial);
        Vec3 look = player.getLookAngle();
        double yawX = look.x, yawZ = look.z;
        double len = Math.sqrt(yawX * yawX + yawZ * yawZ);
        if (len < 1.0e-4) { len = 1.0; yawX = 0.0; yawZ = -1.0; }
        yawX /= len; yawZ /= len;
        // 舞台右方向 = 前进方向绕上轴 -90°
        double rightX = -yawZ, rightZ = yawX;
        double px = anchor.x, py = anchor.y, pz = anchor.z;
        if (p != null) {
            // 与 drawStage 相同的舞台倾角（pose 空间：x 右、y 上、z 远离摄像机），
            // 火花才会打在看得见的部件位置上。
            org.joml.Vector3f v = new org.joml.Vector3f(p.x(), p.y(), -p.z());
            v.rotateY((float) Math.toRadians(STAGE_TILT_Y));
            v.rotateX((float) Math.toRadians(STAGE_TILT_X));
            px += rightX * v.x + yawX * v.z;
            py += v.y + look.y * v.z;
            pz += rightZ * v.x + yawZ * v.z;
        }
        for (int i = 0; i < count; i++) {
            mc.level.addParticle(ParticleTypes.END_ROD,
                    px + (mc.level.random.nextDouble() - 0.5) * spread,
                    py + (mc.level.random.nextDouble() - 0.5) * spread * 0.8,
                    pz + (mc.level.random.nextDouble() - 0.5) * spread,
                    0.0, 0.02, 0.0);
        }
    }

    /** 平滑进度：age 在 [from,to] 间取 0..1，两端用 smoothstep 缓动。 */
    private static float ease(float age, float from, float to) {
        float t = Mth.clamp((age - from) / (to - from), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
