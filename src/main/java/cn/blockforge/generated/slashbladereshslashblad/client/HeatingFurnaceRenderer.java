package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.blockentity.HeatingFurnaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 烧铁炉显示：炉口悬浮画着正在烧的物品。
 * 按烧制进度连续染红同一模型，同时提高方块光照；刀条烧好后若一直不夹出，
 * 颜色会在 5 秒宽限里从暗红渐变成发光的橙黄（过度动画），到时即过火报废。
 */
public class HeatingFurnaceRenderer implements BlockEntityRenderer<HeatingFurnaceBlockEntity> {
    public HeatingFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HeatingFurnaceBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        ItemStack stored = be.items().getItem(0);
        if (stored.isEmpty()) {
            return;
        }
        float overburn = be.overburn(level, partialTick);
        float heat = be.failedStored() ? Math.max(0.15f, overburn) : be.heat(level, partialTick);
        if (overburn > 0f) {
            heat = 1f;
        }
        ItemStack display = HeatingFurnaceBlockEntity.coldDisplay(stored);
        int blockLight = combinedLight >> 4 & 15;
        int skyLight = combinedLight >> 20 & 15;
        // 橙黄阶段让亮度轻微脉动，看起来像在发光的炉温。
        float glow = Math.max(heat, overburn * (0.92f + 0.08f * (float) Math.sin(
                (level.getGameTime() + partialTick) * 0.35)));
        int light = LightTexture.pack(Math.round(blockLight + (15 - blockLight) * glow), skyLight);
        MultiBufferSource tinted = HeatTint.wrap(bufferSource, heat, overburn);
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        float rotation = switch (be.getBlockState().getValue(
                cn.blockforge.generated.slashbladereshslashblad.block.HeatingFurnaceBlock.FACING)) {
            case EAST -> -90f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        };
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, 0, -0.5);
        int count = Math.min(stored.getCount(), HeatingFurnaceBlockEntity.BATCH_MAX);
        for (int i = 0; i < count; i++) {
            double x = count == 1 ? 0.5 : 0.34 + (i % 3) * 0.16;
            double z = count == 1 ? 0.40 : 0.30 + (i / 3) * 0.24;
            FloatingItem.draw(level, display, x, 0.65, z, count == 1 ? 0.34f : 0.19f,
                    light, combinedOverlay, poseStack, tinted, i + 7);
        }
        poseStack.popPose();
        BlockPos pos = be.getBlockPos();
        if (heat > 0.4 && level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    pos.getX() + 0.3 + level.random.nextDouble() * 0.4, pos.getY() + 0.6,
                    pos.getZ() + 0.3 + level.random.nextDouble() * 0.4, 0, 0.01, 0);
        }
        if (be.done() && level.random.nextInt(9) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
