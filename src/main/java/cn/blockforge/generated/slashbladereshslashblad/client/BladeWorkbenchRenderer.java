package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.BladeWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 刀剑制作台展示：普通部件平放，拔刀剑横向出鞘，耀魂材料使用更立体的展示模型。 */
public class BladeWorkbenchRenderer implements BlockEntityRenderer<BladeWorkbenchBlockEntity> {
    public BladeWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BladeWorkbenchBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;
        SimpleContainer inv = be.items();
        for (int slot = 0; slot < BladeWorkbenchBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            double x = 0.16 + (slot % 6) * 0.135;
            double z = 0.23 + (slot / 6) * 0.27;
            if (BladeData.isSlashBlade(stack)) {
                BladeOnAnvil.draw(stack, 1.025, poseStack, bufferSource, combinedLight);
            } else if (BladeData.isProudSoul(stack)) {
                ForgeWorkbenchRenderer.drawSoul(stack, x, z, 0, poseStack, bufferSource, combinedLight, combinedOverlay);
            } else {
                FloatingItem.resting(level, stack, x, 0.91, z, 0.27f,
                        combinedLight, combinedOverlay, poseStack, bufferSource, slot + 20);
            }
        }
    }
}
