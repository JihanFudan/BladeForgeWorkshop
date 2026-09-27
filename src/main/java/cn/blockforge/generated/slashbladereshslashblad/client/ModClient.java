package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCutClient;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** 客户端注册：三个工位的方块实体渲染器（台面悬浮显示物品）与检视按键。 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModClient {
    static {
        NeoForge.EVENT_BUS.register(UltimateJudgementCutClient.class);
        NeoForge.EVENT_BUS.register(BladeInspectClient.class);
        NeoForge.EVENT_BUS.register(BladeAssemblyClient.class);
    }

    private ModClient() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BladeInspectClient.INSPECT_KEY);
    }

    @SubscribeEvent
    public static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(ForgeWorkbenchRenderer.soulModel("soul_fragment"));
        event.register(ForgeWorkbenchRenderer.soulModel("soul_ingot"));
        event.register(ForgeWorkbenchRenderer.soulModel("soul_sphere"));
        HeldBladeModels.registerIcons(event);
    }

    /** 刀条 / 刀鞘拿在手里的模型：换成拔刀剑本体的刀刃、刀鞘网格。 */
    @SubscribeEvent
    public static void registerItemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        HeldBladeModels.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GeneratedMod.FORGE_WORKBENCH_ENTITY.get(), ForgeWorkbenchRenderer::new);
        event.registerBlockEntityRenderer(GeneratedMod.BLADE_WORKBENCH_ENTITY.get(), BladeWorkbenchRenderer::new);
        event.registerBlockEntityRenderer(GeneratedMod.HEATING_FURNACE_ENTITY.get(), HeatingFurnaceRenderer::new);
    }
}
