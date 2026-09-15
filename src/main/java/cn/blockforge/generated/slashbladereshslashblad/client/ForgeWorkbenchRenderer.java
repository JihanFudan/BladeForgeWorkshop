package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.ForgeWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 工件、原料和工具分别平放，耀魂材料使用铁砧专用立体模型。 */
public class ForgeWorkbenchRenderer implements BlockEntityRenderer<ForgeWorkbenchBlockEntity> {
    public ForgeWorkbenchRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ForgeWorkbenchBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        var inv = be.items();
        boolean anvil = be.getBlockState().is(GeneratedMod.FORGE_ANVIL.get());
        float elapsed = level.getGameTime() - be.lastHit() + partialTick;
        double sink = elapsed >= 0 && elapsed < 8 ? -0.015 * Math.sin(elapsed / 8 * Math.PI) : 0;
        if (anvil && BladeData.isSlashBlade(inv.getItem(0))) {
            BladeOnAnvil.draw(inv.getItem(0), 1.025 + sink, pose, buffers, light);
        } else {
            FloatingItem.resting(level, inv.getItem(0), 0.5, 1.035 + sink, 0.49, 0.48f,
                    light, overlay, pose, buffers, 1);
        }
        for (int slot = 1; slot <= 2; slot++) {
            ItemStack stack = inv.getItem(slot);
            int count = Math.min(stack.getCount(), 5);
            for (int i = 0; i < count; i++) {
                double x = slot == 1 ? 0.22 : 0.78;
                FloatingItem.resting(level, stack, x, 1.035 + i * 0.018 + sink, 0.5, 0.25f,
                        light, overlay, pose, buffers, slot * 10 + i);
            }
        }
        if (anvil && BladeData.isProudSoul(inv.getItem(3))) {
            drawSoul(inv.getItem(3), sink, pose, buffers, light, overlay);
        } else {
            FloatingItem.resting(level, inv.getItem(3), 0.5, 1.035, 0.23, 0.25f,
                    light, overlay, pose, buffers, 4);
        }
        // 刀镡制作台只显示正在加工的材料和成品，不把切割刀、雕刻凿画在台面上。
    }

    public static ModelResourceLocation soulModel(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "block/" + name));
    }

    static void drawSoul(ItemStack stack, double sink, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        drawSoul(stack, 0.3, 0.05, sink, pose, buffers, light, overlay);
    }

    static void drawSoul(ItemStack stack, double x, double z, double sink, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        String name = switch (BladeData.proudSoulValue(stack)) {
            case 400 -> "soul_ingot";
            case 1000 -> "soul_sphere";
            default -> "soul_fragment";
        };
        Minecraft mc = Minecraft.getInstance();
        var model = mc.getModelManager().getModel(soulModel(name));
        pose.pushPose();
        try {
            pose.translate(x, 1.01 + sink, z);
            pose.scale(0.4f, 0.4f, 0.4f);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.solid()),
                    null, model, 1f, 1f, 1f, light, overlay);
        } finally { pose.popPose(); }
    }
}
