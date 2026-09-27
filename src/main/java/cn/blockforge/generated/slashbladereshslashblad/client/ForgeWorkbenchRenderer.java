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
        // 融合槽三格（slot 1 钢、slot 5 低碳钢、slot 6 高碳钢）从左到右分堆；炼钢槽（slot 2）在右侧。
        int[] ingotSlots = {1, 5, 6, 2};
        double[] ingotX = {0.18, 0.32, 0.46, 0.78};
        for (int s = 0; s < ingotSlots.length; s++) {
            ItemStack stack = inv.getItem(ingotSlots[s]);
            int count = Math.min(stack.getCount(), 5);
            for (int i = 0; i < count; i++) {
                // 烧红锭材在砧面上放大显示（之前 0.25 太小，用户看不清是什么）
                FloatingItem.resting(level, stack, ingotX[s], 1.035 + i * 0.02 + sink, 0.5, 0.4f,
                        light, overlay, pose, buffers, ingotSlots[s] * 10 + i);
            }
        }
        // 煤炭槽（slot 4）：等待研磨成碳粉的煤炭，摆在砧面前缘。
        if (anvil && !inv.getItem(4).isEmpty()) {
            FloatingItem.resting(level, inv.getItem(4), 0.5, 1.02 + sink, 0.78, 0.34f,
                    light, overlay, pose, buffers, 90);
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
