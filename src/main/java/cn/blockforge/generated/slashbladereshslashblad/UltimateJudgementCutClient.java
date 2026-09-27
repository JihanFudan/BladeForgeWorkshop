package cn.blockforge.generated.slashbladereshslashblad;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 客户端究极次元斩演出：蓝色领域、空间裂隙、挥刀残影和空间破碎。 */
public final class UltimateJudgementCutClient {
    private static final List<ClientEffect> EFFECTS = new ArrayList<>();
    public static int motionEntity = -1;
    public static float motionPartial;

    public static float animationAge(int entityId, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        for (ClientEffect effect : EFFECTS) {
            float age = mc.level.getGameTime() - effect.startGameTime + partial;
            if (effect.casterEntityId == entityId && age >= 0 && age < UltimateJudgementCut.DURATION_TICKS) return age;
        }
        return -1;
    }
    private static final int SHATTER_TAIL_TICKS = 12;
    private static final ByteBufferBuilder EFFECT_BUFFER = new ByteBufferBuilder(786432);
    private static final MultiBufferSource.BufferSource EFFECT_BUFFERS = MultiBufferSource.immediate(EFFECT_BUFFER);

    private UltimateJudgementCutClient() {
    }

    /** 本地也立刻清空移动输入，避免服务端回弹前出现一小段可走动的错觉。 */
    @SubscribeEvent
    public static void lockLocalCasterMovement(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        for (ClientEffect effect : EFFECTS) {
            if (effect.casterEntityId != minecraft.player.getId()
                    || now - effect.startGameTime > UltimateJudgementCut.DURATION_TICKS) continue;
            minecraft.player.setDeltaMovement(Vec3.ZERO);
            minecraft.player.input.leftImpulse = 0.0f;
            minecraft.player.input.forwardImpulse = 0.0f;
            minecraft.player.input.jumping = false;
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyRight.setDown(false);
            minecraft.options.keyJump.setDown(false);
            minecraft.options.keySprint.setDown(false);
            return;
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        EFFECTS.clear();
        motionEntity = -1;
        motionPartial = 0;
    }

    public static void start(UltimateJudgementCutNetwork.StartPayload payload) {
        if (Minecraft.getInstance().level == null) return;
        EFFECTS.removeIf(effect -> effect.casterEntityId == payload.casterEntityId());
        EFFECTS.add(new ClientEffect(payload));
    }

    /** 服务端通知 SSA 被检视打断：立即撤掉本地演出与咏唱锁定。 */
    public static void cancel(int casterEntityId) {
        EFFECTS.removeIf(effect -> effect.casterEntityId == casterEntityId);
    }

    /** 在原版与拔刀剑处理右键前取消普通 SA，并向服务器请求专属 SSA。 */
    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND
                || minecraft.player == null || minecraft.screen != null) return;
        if (!minecraft.options.keyUp.isDown() || !minecraft.options.keyDown.isDown()) return;
        if (!UltimateJudgementCut.isYamato(minecraft.player.getMainHandItem())) return;

        event.setSwingHand(false);
        event.setCanceled(true);
        // 重锋会在同一 Tick 独立读取 keyUse；提前释放可阻止它生成普通 R_DOWN/SA。
        minecraft.options.keyUse.setDown(false);
        UltimateJudgementCutNetwork.requestStart();
    }

    @SubscribeEvent
    public static void addYamatoTooltip(ItemTooltipEvent event) {
        if (!UltimateJudgementCut.isYamato(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("tooltip.slashbladeresh_slashblad.yamato_ssa")
                .withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("tooltip.slashbladeresh_slashblad.yamato_ssa_trigger")
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("tooltip.slashbladeresh_slashblad.yamato_ssa_cost")
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.slashbladeresh_slashblad.yamato_ssa_targets")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @SubscribeEvent
    public static void renderBlueDomain(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        EFFECTS.removeIf(effect -> now - effect.startGameTime
                > UltimateJudgementCut.DURATION_TICKS + SHATTER_TAIL_TICKS);
        int strongestAlpha = 0;
        float strongestFracture = 0.0f;
        for (ClientEffect effect : EFFECTS) {
            float age = now - effect.startGameTime + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            if (!effect.visibleTo(minecraft.player.position(), age)) continue;
            float fadeIn = Mth.clamp(age / 3.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((UltimateJudgementCut.DURATION_TICKS + SHATTER_TAIL_TICKS - age)
                    / SHATTER_TAIL_TICKS, 0.0f, 1.0f);
            strongestAlpha = Math.max(strongestAlpha, (int) (72.0f * fadeIn * fadeOut));
            strongestFracture = Math.max(strongestFracture,
                    Mth.clamp((age - UltimateJudgementCut.IMPACT_TICK) / 2.5f, 0.0f, 1.0f) * fadeOut);
        }
        if (strongestAlpha > 0) {
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                    event.getGuiGraphics().guiHeight(), strongestAlpha << 24 | 0x082C68);
        }
        if (strongestFracture > 0.0f) renderScreenFractures(event, strongestFracture, now);
    }

    /** 末段的近景裂纹：细亮缝与半透明镜面碎片叠在蓝色领域上。 */
    private static void renderScreenFractures(RenderGuiEvent.Post event, float strength, long now) {
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        java.util.Random random = new java.util.Random(now * 17L + 0x5EEDL);
        int shardAlpha = (int) (48 * strength);
        int riftAlpha = (int) (185 * strength);
        for (int i = 0; i < 18; i++) {
            int x = random.nextInt(Math.max(1, width));
            int y = random.nextInt(Math.max(1, height));
            int horizontal = 7 + random.nextInt(Math.max(8, width / 8));
            int vertical = 5 + random.nextInt(Math.max(6, height / 9));
            event.getGuiGraphics().fill(x, y, Math.min(width, x + horizontal), Math.min(height, y + vertical),
                    shardAlpha << 24 | 0x1D73B7);
            event.getGuiGraphics().fill(x, y, Math.min(width, x + horizontal), Math.min(height, y + 1),
                    riftAlpha << 24 | 0xC5F4FF);
            event.getGuiGraphics().fill(x, y, Math.min(width, x + 1), Math.min(height, y + vertical),
                    riftAlpha << 24 | 0x8ADFFF);
        }
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || EFFECTS.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = minecraft.level.getGameTime();
        Iterator<ClientEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            ClientEffect effect = iterator.next();
            float age = now - effect.startGameTime + partial;
            if (age > UltimateJudgementCut.DURATION_TICKS + SHATTER_TAIL_TICKS) {
                iterator.remove();
                continue;
            }
            effect.render(event, age, minecraft);
        }
    }

    private static final class ClientEffect {
        private static final int SLASH_COUNT = 20;
        private static final int SPHERE_SEGMENTS = 48;
        private final int casterEntityId;
        private final Vec3 center;
        private final float yaw;
        private final long startGameTime;

        private ClientEffect(UltimateJudgementCutNetwork.StartPayload payload) {
            this.casterEntityId = payload.casterEntityId();
            this.center = payload.center();
            this.yaw = payload.yaw();
            this.startGameTime = payload.startGameTime();
        }

        private boolean visibleTo(Vec3 viewer, float age) {
            return age >= 1.0f && age <= UltimateJudgementCut.DURATION_TICKS + SHATTER_TAIL_TICKS
                    && viewer.distanceToSqr(center) <= UltimateJudgementCut.RADIUS * UltimateJudgementCut.RADIUS;
        }

        private void render(RenderLevelStageEvent event, float age, Minecraft minecraft) {
            if (age < 1.0f) return;
            Vec3 camera = event.getCamera().getPosition();
            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = EFFECT_BUFFERS;
            renderBoundary(age, camera, pose, buffers.getBuffer(RenderType.lightning()));

            // 每次释放生成不同布局；同一次释放每帧使用同一随机种子，避免线条跳动。
            java.util.Random random = new java.util.Random(startGameTime * 31L + casterEntityId);
            for (int i = 0; i < SLASH_COUNT; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double vertical = random.nextDouble() * 2.0 - 1.0;
                double horizontal = Math.sqrt(1.0 - vertical * vertical);
                Vec3 radial = new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();
                Vec3 second = radial.cross(tangent).normalize();
                double offsetAngle = random.nextDouble() * Math.PI * 2;
                double offsetRadius = Math.sqrt(random.nextDouble()) * UltimateJudgementCut.RADIUS * 0.85;
                Vec3 offset = tangent.scale(Math.cos(offsetAngle) * offsetRadius)
                        .add(second.scale(Math.sin(offsetAngle) * offsetRadius));
                double halfLength = Math.sqrt(UltimateJudgementCut.RADIUS * UltimateJudgementCut.RADIUS
                        - offset.lengthSqr());
                Vec3 start = center.add(offset).subtract(radial.scale(halfLength));
                Vec3 fullEnd = center.add(offset).add(radial.scale(halfLength));
                // 第 1—5 tick 随机出现，飞行与淡出共 5 tick，全部在第 10 tick 前完成。
                float slashStart = 1.0f + random.nextFloat() * 4.0f;
                float reveal = age >= slashStart ? 1.0f : 0.0f;
                float shatterFade = Mth.clamp((UltimateJudgementCut.IMPACT_TICK + 3.0f - age) / 3.0f,
                        0.0f, 1.0f);
                if (reveal <= 0.0f || shatterFade <= 0.0f) continue;

                int alpha = (int) (245 * shatterFade);
                VertexConsumer rifts = buffers.getBuffer(RenderType.lightning());
                drawBlade(pose, rifts, start.subtract(camera), fullEnd.subtract(camera), 0.018f,
                        120, 190, 255, alpha);
                drawBlade(pose, rifts, start.subtract(camera), fullEnd.subtract(camera), 0.007f,
                        255, 255, 255, alpha);
                drawBladeCross(pose, rifts, start.subtract(camera), fullEnd.subtract(camera), 0.018f,
                        120, 190, 255, alpha);
                drawBladeCross(pose, rifts, start.subtract(camera), fullEnd.subtract(camera), 0.007f,
                        255, 255, 255, alpha);

                float afterimageTravel = (age - slashStart) / 4.0f;
                renderAfterimage(i, afterimageTravel, start, fullEnd, tangent, (float) Math.toDegrees(angle),
                        camera, pose, buffers, minecraft);
            }

            if (age >= UltimateJudgementCut.IMPACT_TICK) {
                renderShatter(age - UltimateJudgementCut.IMPACT_TICK, camera, pose,
                        buffers.getBuffer(RenderType.lightning()));
            }
            buffers.endBatch();
        }

        private void renderBoundary(float age, Vec3 camera, PoseStack pose, VertexConsumer buffer) {
            float fadeIn = Mth.clamp(age / 4.0f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((UltimateJudgementCut.DURATION_TICKS - age) / 6.0f, 0.0f, 1.0f);
            float pulse = 0.88f + 0.12f * Mth.sin(age * 0.75f);
            int alpha = (int) (150 * fadeIn * fadeOut);
            double radius = UltimateJudgementCut.RADIUS * pulse;
            Vec3 relativeCenter = center.subtract(camera);

            double latitudeRadius = radius * 0.866;
            drawRing(pose, buffer, relativeCenter.add(0, radius * 0.5, 0),
                    new Vec3(1, 0, 0), new Vec3(0, 0, 1), latitudeRadius, 0.04f, alpha * 2 / 3);
            drawRing(pose, buffer, relativeCenter.add(0, -radius * 0.5, 0),
                    new Vec3(1, 0, 0), new Vec3(0, 0, 1), latitudeRadius, 0.04f, alpha * 2 / 3);
        }

        private void drawRing(PoseStack pose, VertexConsumer buffer, Vec3 ringCenter, Vec3 axisA, Vec3 axisB,
                              double radius, float width, int alpha) {
            Vec3 previous = ringCenter.add(axisA.scale(radius));
            for (int segment = 1; segment <= SPHERE_SEGMENTS; segment++) {
                double angle = segment * Math.PI * 2.0 / SPHERE_SEGMENTS;
                Vec3 next = ringCenter.add(axisA.scale(Math.cos(angle) * radius))
                        .add(axisB.scale(Math.sin(angle) * radius));
                drawBlade(pose, buffer, previous, next, width, 54, 168, 255, alpha);
                previous = next;
            }
        }

        private Vec3 slashPoint(Vec3 start, Vec3 end, Vec3 tangent, float t) {
            return start.lerp(end, t);
        }

        private void renderAfterimage(int index, float travel, Vec3 start, Vec3 end, Vec3 tangent,
                                      float slashYaw,
                                      Vec3 camera, PoseStack pose, MultiBufferSource buffers, Minecraft minecraft) {
            if (travel < 0.0f || travel > 1.18f || minecraft.level == null) return;
            if (!(minecraft.level.getEntity(casterEntityId) instanceof AbstractClientPlayer caster)) return;
            EntityRenderer<? super AbstractClientPlayer> renderer = minecraft.getEntityRenderDispatcher().getRenderer(caster);
            if (!(renderer instanceof PlayerRenderer playerRenderer)) return;

            float clampedTravel = Mth.clamp(travel, 0.0f, 1.0f);
            float fadeIn = Mth.clamp(clampedTravel / 0.12f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((1.18f - travel) / 0.18f, 0.0f, 1.0f);
            float fade = Math.min(fadeIn, fadeOut);
            Vec3 ghost = slashPoint(start, end, tangent, clampedTravel);
            pose.pushPose();
            pose.translate(ghost.x - camera.x, ghost.y - camera.y - caster.getBbHeight() * 0.5, ghost.z - camera.z);
            Vec3 flight = end.subtract(start).normalize();
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(flight.x, -flight.z))));
            pose.mulPose(Axis.XP.rotationDegrees((float) -Math.toDegrees(Math.asin(flight.y))));
            pose.scale(-1.0f, -1.0f, 1.0f);
            pose.translate(0.0, -1.501, 0.0);

            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
            List<ModelPart> modelParts = List.of(model.head, model.hat, model.body, model.rightArm,
                    model.leftArm, model.rightLeg, model.leftLeg, model.rightSleeve, model.leftSleeve,
                    model.rightPants, model.leftPants, model.jacket);
            List<PartPose> savedPoses = modelParts.stream().map(ModelPart::storePose).toList();
            boolean savedRiding = model.riding;
            boolean savedYoung = model.young;
            boolean savedCrouching = model.crouching;
            float savedAttackTime = model.attackTime;
            var savedRightPose = model.rightArmPose;
            var savedLeftPose = model.leftArmPose;
            try {
                model.riding = false;
                model.young = false;
                model.crouching = false;
                model.attackTime = 0.9f;
                model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
                model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
                model.prepareMobModel(caster, 0.0f, 0.0f, 0.0f);
                model.setupAnim(caster, 0.0f, 0.0f, caster.tickCount, 0.0f, 0.0f);
                float swing = Mth.sin(clampedTravel * Mth.PI);
                float slashPhase = Mth.sin(clampedTravel * Mth.PI * 1.35f);
                model.rightArm.xRot = -2.25f + 1.45f * swing;
                model.rightArm.yRot = (index % 2 == 0 ? -0.9f : 0.35f) + 0.35f * slashPhase;
                model.rightArm.zRot = -0.18f * slashPhase;
                model.leftArm.xRot = -0.75f + 0.45f * swing;
                int alpha = (int) (150 * fade);
                int color = alpha << 24 | 0x75D8FF;
                VertexConsumer ghostBuffer = buffers.getBuffer(RenderType.entityTranslucent(caster.getSkin().texture()));
                model.renderToBuffer(pose, ghostBuffer, LightTexture.FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);

                pose.pushPose();
                model.translateToHand(net.minecraft.world.entity.HumanoidArm.RIGHT, pose);
                pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
                pose.mulPose(Axis.YP.rotationDegrees(180.0f));
                pose.translate(1.0f / 16.0f, 0.125f, -0.625f);
                minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(caster,
                        caster.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false,
                        pose, buffers, LightTexture.FULL_BRIGHT);
                pose.popPose();
            } finally {
                Iterator<PartPose> poses = savedPoses.iterator();
                modelParts.forEach(part -> part.loadPose(poses.next()));
                model.riding = savedRiding;
                model.young = savedYoung;
                model.crouching = savedCrouching;
                model.attackTime = savedAttackTime;
                model.rightArmPose = savedRightPose;
                model.leftArmPose = savedLeftPose;
                pose.popPose();
            }
        }

        /** 每一条已出现的斩痕都会沿路径裂成镜片，而非只在领域中心爆出一团粒子。 */
        private void renderShatter(float impactAge, Vec3 camera, PoseStack pose, VertexConsumer buffer) {
            float expansion = Mth.clamp(impactAge / 5.0f, 0.0f, 1.0f);
            float alphaScale = Mth.clamp((12.0f - impactAge) / 8.0f, 0.0f, 1.0f);
            java.util.Random layout = new java.util.Random(startGameTime * 31L + casterEntityId);
            java.util.Random shards = new java.util.Random((startGameTime * 31L + casterEntityId) ^ 0x51A77E2L);
            for (int i = 0; i < SLASH_COUNT; i++) {
                double angle = layout.nextDouble() * Math.PI * 2;
                double vertical = layout.nextDouble() * 2.0 - 1.0;
                double horizontal = Math.sqrt(1.0 - vertical * vertical);
                Vec3 radial = new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();
                Vec3 second = radial.cross(tangent).normalize();
                double offsetAngle = layout.nextDouble() * Math.PI * 2;
                double offsetRadius = Math.sqrt(layout.nextDouble()) * UltimateJudgementCut.RADIUS * 0.85;
                Vec3 offset = tangent.scale(Math.cos(offsetAngle) * offsetRadius)
                        .add(second.scale(Math.sin(offsetAngle) * offsetRadius));
                double halfLength = Math.sqrt(UltimateJudgementCut.RADIUS * UltimateJudgementCut.RADIUS
                        - offset.lengthSqr());
                Vec3 start = center.add(offset).subtract(radial.scale(halfLength));
                Vec3 end = center.add(offset).add(radial.scale(halfLength));
                layout.nextFloat(); // 与斩痕布局的出刀随机数对齐。
                for (int fragment = 0; fragment < 4; fragment++) {
                    float t = (fragment + 0.18f + shards.nextFloat() * 0.58f) / 4.0f;
                    Vec3 anchor = start.lerp(end, t);
                    Vec3 flight = anchor.subtract(center).normalize().scale(
                            expansion * (0.45 + shards.nextDouble() * 1.35));
                    Vec3 wingA = tangent.scale(0.10 + shards.nextDouble() * 0.20);
                    Vec3 wingB = second.scale((shards.nextDouble() - 0.5) * 0.34);
                    Vec3 tip = anchor.add(flight).add(wingA).add(wingB);
                    Vec3 left = anchor.add(flight.scale(0.58)).subtract(wingA).add(wingB.scale(0.45));
                    Vec3 right = anchor.add(flight.scale(0.66)).add(second.scale(0.12 + shards.nextDouble() * 0.16));
                    int alpha = (int) (225 * alphaScale);
                    drawBlade(pose, buffer, anchor.subtract(camera), tip.subtract(camera), 0.030f,
                            131, 222, 255, alpha);
                    drawBlade(pose, buffer, tip.subtract(camera), left.subtract(camera), 0.021f,
                            205, 246, 255, alpha);
                    drawBlade(pose, buffer, tip.subtract(camera), right.subtract(camera), 0.021f,
                            205, 246, 255, alpha);
                }
            }
        }
    }

    private static void drawBlade(PoseStack pose, VertexConsumer buffer, Vec3 start, Vec3 end, float width,
                                  int red, int green, int blue, int alpha) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 perpendicular = direction.cross(start.add(end).scale(-0.5));
        if (perpendicular.lengthSqr() < 0.001) perpendicular = new Vec3(1.0, 0.0, 0.0);
        perpendicular = perpendicular.normalize().scale(width);
        Vec3 a = start.add(perpendicular);
        Vec3 b = start.subtract(perpendicular);
        Vec3 c = end.subtract(perpendicular);
        Vec3 d = end.add(perpendicular);
        addVertex(pose, buffer, a, red, green, blue, alpha);
        addVertex(pose, buffer, b, red, green, blue, alpha);
        addVertex(pose, buffer, c, red, green, blue, alpha);
        addVertex(pose, buffer, d, red, green, blue, alpha);
        addVertex(pose, buffer, d, red, green, blue, alpha);
        addVertex(pose, buffer, c, red, green, blue, alpha);
        addVertex(pose, buffer, b, red, green, blue, alpha);
        addVertex(pose, buffer, a, red, green, blue, alpha);
    }

    private static void drawBladeCross(PoseStack pose, VertexConsumer buffer, Vec3 start, Vec3 end, float width,
                                      int red, int green, int blue, int alpha) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 perpendicular = direction.cross(new Vec3(1.0, 0.0, 0.0));
        if (perpendicular.lengthSqr() < 0.001) perpendicular = direction.cross(new Vec3(0.0, 0.0, 1.0));
        perpendicular = perpendicular.normalize().scale(width);
        addVertex(pose, buffer, start.add(perpendicular), red, green, blue, alpha);
        addVertex(pose, buffer, start.subtract(perpendicular), red, green, blue, alpha);
        addVertex(pose, buffer, end.subtract(perpendicular), red, green, blue, alpha);
        addVertex(pose, buffer, end.add(perpendicular), red, green, blue, alpha);
    }

    private static void addVertex(PoseStack pose, VertexConsumer buffer, Vec3 point,
                                  int red, int green, int blue, int alpha) {
        buffer.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha);
    }
}
