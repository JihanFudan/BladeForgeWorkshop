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
 * 按烧制进度连续染红同一模型，同时提高方块光照；完成后保持相同外观。
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
        float heat = be.heat(level, partialTick);
        ItemStack display = HeatingFurnaceBlockEntity.coldDisplay(stored);
        int blockLight = combinedLight >> 4 & 15;
        int skyLight = combinedLight >> 20 & 15;
        int light = LightTexture.pack(Math.round(blockLight + (15 - blockLight) * heat), skyLight);
        MultiBufferSource tinted = HeatTint.wrap(bufferSource, heat);
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
