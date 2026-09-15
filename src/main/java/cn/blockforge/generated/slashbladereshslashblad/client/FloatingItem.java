package cn.blockforge.generated.slashbladereshslashblad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** 工位悬浮显示通用工具：把台面上的物品用 FIXED 姿态画在方块坐标里，带轻微上下浮动。 */
final class FloatingItem {
    private FloatingItem() {
    }

    static void resting(Level level, ItemStack stack, double x, double y, double z, float scale,
                        int light, int overlay, PoseStack poseStack, MultiBufferSource buffers, int seed) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                light, overlay, poseStack, buffers, level, seed);
        poseStack.popPose();
    }

    static void draw(Level level, ItemStack stack, double x, double y, double z, float scale,
                     int light, int overlay, PoseStack poseStack, MultiBufferSource bufferSource, int seed) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x, y + Math.sin((level.getGameTime() % 200) * 0.055 + seed * 1.7) * 0.018, z);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((level.getGameTime() % 360) * 1.0f));
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer()
                .renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed);
        poseStack.popPose();
    }
}
