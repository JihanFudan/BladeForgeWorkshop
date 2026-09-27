package cn.blockforge.generated.slashbladereshslashblad.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;

/**
 * 刀条 / 刀鞘"拿在手中"的模型：直接画拔刀剑本体的网格分组。
 *
 * <ul>
 *   <li>刀鞘 —— 木偶的刀鞘（blade.obj 的 sheath 分组 + 木偶 wood.png 材质表）</li>
 *   <li>金属刀条 —— 大太刀的刀刃（blade 分组的钢刃）</li>
 *   <li>木刀条 —— 木偶的刀刃（同一分组 + 木偶 wood.png 材质）</li>
 *   <li>竹刀条 —— 竹光的刀刃（同一分组 + 竹光 bamboo.png 材质）</li>
 * </ul>
 *
 * <p>物品模型 JSON 用 {@code builtin/entity} 挂上本类的渲染器，位置/朝向完全
 * 交给 JSON 里的 {@code display} 段——数值照抄原版 item/generated 与
 * item/handheld，只把手持的 scale 放大到拔刀剑那种"刀比人长"的分量。这样
 * 左右手、副手、掉落、展示框都走原版同一套变换，不会出现刀插进手臂的情况。</p>
 *
 * <p>背包格子（GUI）继续用原来的平面图标：真实比例的日本刀刀身只有 0.04 格宽，
 * 缩到 16 像素的格子里会糊成一条线，平面图标反而看得清。手上、地上、展示框里
 * 画的是 3D 网格。平面图标本身是一份 {@code item/generated} 的伴生模型
 * （{@code models/item/<name>_icon.json}），直接借原版烘好的几何来画，
 * 位置、尺寸、留白和别的物品一模一样。</p>
 */
public final class HeldBladeModels {
    private HeldBladeModels() {
    }

    private static final String NS = GeneratedMod.MOD_ID;

    /** 手持网格部件 + 材质表 + 乘色 + 平面图标模型。
     *
     * <p>{@code gripWork}/{@code gripTool} 是"背包里有钳子"时改画的部件：
     * 工件换成以钳子为中心摆放的那一份（{@code gripWork}），再加一把钳子
     * （{@code gripTool}）。两件都为空表示这件物品不会被夹。</p>
     *
     * <p>{@code flatIngot} 标记锭子网格：它被平放在工位台面上（FIXED 视角）时
     * 换用宽面朝上的 {@code hold_ingot_flat} 部件，而不是侧着立起来。</p> */
    private record Held(String part, ResourceLocation sheet, float r, float g, float b, ModelResourceLocation icon,
                        String gripWork, String gripTool, boolean flatIngot) {
        /** 这件物品会不会被钳子夹起来。 */
        boolean grippable() {
            return gripTool != null;
        }
    }

    private static final Map<Item, Held> HELD = new HashMap<>();
    private static final List<ModelResourceLocation> ICON_MODELS = new ArrayList<>();

    /** 钳子本身的材质表（sb_iron.png）。 */
    private static final ResourceLocation TONGS_SHEET = assemblyTex("sb_iron");

    private static ResourceLocation assemblyTex(String name) {
        return ResourceLocation.fromNamespaceAndPath(NS, "textures/assembly/" + name + ".png");
    }

    private static ModelResourceLocation iconModel(String name) {
        ModelResourceLocation mrl = ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(NS, "item/" + name + "_icon"));
        ICON_MODELS.add(mrl);
        return mrl;
    }

    /** 登记一件物品：手持用哪个部件、哪张材质表、乘什么色、格子用什么图标。 */
    private static void hold(Item item, String part, String sheet, float r, float g, float b, String icon) {
        HELD.put(item, new Held(part, assemblyTex(sheet), r, g, b, iconModel(icon), null, null, false));
    }

    /** 登记一件"背包里有钳子就夹着"的烧红工件，多给两个部件名（夹持姿态的工件 + 钳子）。 */
    private static void holdGripped(Item item, String part, String sheet, float r, float g, float b, String icon,
                                    String gripWork, String gripTool) {
        HELD.put(item, new Held(part, assemblyTex(sheet), r, g, b, iconModel(icon), gripWork, gripTool, false));
    }

    /** 同上，再加"工位台面上宽面朝上平放"（锭子专用网格）。 */
    private static void holdGrippedFlat(Item item, String part, String sheet, float r, float g, float b, String icon,
                                        String gripWork, String gripTool) {
        HELD.put(item, new Held(part, assemblyTex(sheet), r, g, b, iconModel(icon), gripWork, gripTool, true));
    }

    /**
     * 建表。必须在物品注册之后调用（{@link #register} / {@link #registerIcons} 都在
     * mod 总线的事件里跑，那时物品已经注册完）。
     */
    private static void build() {
        if (!HELD.isEmpty()) {
            return;
        }
        // 大太刀材质表 sb_oodachi.png（刀刃）、木偶 sb_wood.png（木刀刃 + 刀鞘）、竹光 sb_bamboo.png
        // 刀鞘用木偶的刀鞘：blade.obj 的 sheath 分组 + 木偶 wood.png 材质表
        hold(GeneratedMod.BLADE_SHEATH.get(), "hold_sheath", "sb_wood", 1, 1, 1, "blade_sheath");
        hold(GeneratedMod.QUENCHED_BLADE.get(), "hold_blade_metal", "sb_oodachi", 1, 1, 1, "quenched_blade");
        hold(GeneratedMod.CRUDE_BLADE.get(), "hold_blade_metal", "sb_oodachi",
                0.78f, 0.77f, 0.74f, "crude_blade");
        hold(GeneratedMod.UNFINISHED_BLADE.get(), "hold_blade_metal", "sb_oodachi",
                0.88f, 0.88f, 0.9f, "unfinished_blade");
        hold(GeneratedMod.CLAY_BLADE.get(), "hold_blade_metal", "sb_oodachi",
                0.96f, 0.92f, 0.84f, "clay_blade");
        // 烧红的三件刀条：各自一张材质表（亮橙红 / 暗红黑斑 / 橙红黏土斑），
        // 背包里有钳子时换成"钳子夹着刀条"（另一套以钳子为中心的网格）
        holdGripped(GeneratedMod.HOT_BLADE.get(), "hold_blade_metal", "sb_hot_blade",
                1, 1, 1, "hot_blade", "hold_blade_grip", "hold_tongs_blade");
        holdGripped(GeneratedMod.HEATED_CRUDE_BLADE.get(), "hold_blade_metal", "sb_hot_crude",
                1, 1, 1, "heated_crude_blade", "hold_blade_grip", "hold_tongs_blade");
        holdGripped(GeneratedMod.HEATED_CLAY_BLADE.get(), "hold_blade_metal", "sb_hot_clay",
                1, 1, 1, "heated_clay_blade", "hold_blade_grip", "hold_tongs_blade");
        // 烧红的锭子：铁/钢/融合钢各自一张材质表；被夹时换成钳子 + 锭子的组合；
        // 放在工位台面上时宽面朝上平放（hold_ingot_flat）
        holdGrippedFlat(GeneratedMod.HEATED_IRON.get(), "hold_ingot", "sb_hot_iron",
                1, 1, 1, "heated_iron", "hold_ingot_grip", "hold_tongs_ingot");
        holdGrippedFlat(GeneratedMod.HEATED_STEEL.get(), "hold_ingot", "sb_hot_steel",
                1, 1, 1, "heated_steel", "hold_ingot_grip", "hold_tongs_ingot");
        // 低碳钢、高碳钢各有自己的材质表（近白热 / 深橙红带碳斑），
        // 不再复用钢锭表乘色——乘色区分出来的三块红锭子肉眼几乎一样。
        holdGrippedFlat(GeneratedMod.HEATED_LOW_CARBON.get(), "hold_ingot", "sb_hot_lowcarbon",
                1, 1, 1, "heated_low_carbon", "hold_ingot_grip", "hold_tongs_ingot");
        holdGrippedFlat(GeneratedMod.HEATED_HIGH_CARBON.get(), "hold_ingot", "sb_hot_highcarbon",
                1, 1, 1, "heated_high_carbon", "hold_ingot_grip", "hold_tongs_ingot");
        holdGrippedFlat(GeneratedMod.HEATED_FUSED_STEEL.get(), "hold_ingot", "sb_hot_fused",
                1, 1, 1, "heated_fused_steel", "hold_ingot_grip", "hold_tongs_ingot");
        hold(GeneratedMod.WOODEN_BLADE_BLANK.get(), "hold_blade_wood", "sb_wood",
                1, 1, 1, "wooden_blade_blank");
        hold(GeneratedMod.BAMBOO_BLADE_BLANK.get(), "hold_blade_bamboo", "sb_bamboo",
                1, 1, 1, "bamboo_blade_blank");
    }

    /** 让伴生的平面图标模型参与烘焙（否则拿不到 BakedModel）。 */
    public static void registerIcons(ModelEvent.RegisterAdditional event) {
        build();
        for (ModelResourceLocation mrl : ICON_MODELS) {
            event.register(mrl);
        }
    }

    /** 把这些物品的手持渲染器换成拔刀剑网格。 */
    public static void register(RegisterClientExtensionsEvent event) {
        build();
        IClientItemExtensions ext = new IClientItemExtensions() {
            final BlockEntityWithoutLevelRenderer renderer = new HeldRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        event.registerItem(ext, HELD.keySet().toArray(new Item[0]));
    }

    private static final class HeldRenderer extends BlockEntityWithoutLevelRenderer {
        HeldRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
            Held held = HELD.get(stack.getItem());
            if (held == null) {
                return;
            }
            if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.HEAD || ctx == ItemDisplayContext.NONE) {
                drawIcon(stack, held.icon(), pose, buffers, light, overlay);
                return;
            }
            // 只有"拿在人手上"的工件才换钳子夹持模型。掉落、展示框、工位台面
            // 悬浮等一律不叠钳子——之前手持钳子时所有烧红金属都被误画成夹着，
            // 就是因为没做这个区分。
            if (held.grippable() && isHandContext(ctx) && hasTongsInInventory()) {
                AssemblyStageModel.render(held.gripWork(), pose, buffers, held.sheet(),
                        held.r(), held.g(), held.b(), light);
                AssemblyStageModel.render(held.gripTool(), pose, buffers, TONGS_SHEET,
                        1, 1, 1, light);
                return;
            }
            // 烧红锭子平放在工位台面上（FIXED 视角）：换宽面朝上的网格，
            // 不再侧着立起来（hold_ingot_flat 网格本身已按台面观感放大过）。
            if (held.flatIngot() && ctx == ItemDisplayContext.FIXED) {
                AssemblyStageModel.render("hold_ingot_flat", pose, buffers, held.sheet(),
                        held.r(), held.g(), held.b(), light);
                return;
            }
            AssemblyStageModel.render(held.part(), pose, buffers, held.sheet(),
                    held.r(), held.g(), held.b(), light);
        }
    }

    /** 是不是"拿在人手上"的渲染上下文（第一/第三人称、主/副手）。 */
    private static boolean isHandContext(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    /** 钳子判定与服务端共用一套：背包含快捷栏 36 格 + 副手，免得夹着姿势和实际掉血对不上。 */
    private static boolean hasTongsInInventory() {
        Player player = Minecraft.getInstance().player;
        return player != null && cn.blockforge.generated.slashbladereshslashblad.ForgeEvents.hasTongsInInventory(player);
    }

    /** 画平面图标：直接借用伴生 item/generated 模型烘好的几何。 */
    private static void drawIcon(ItemStack stack, ModelResourceLocation icon, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getModelManager().getModel(icon);
        if (model == null) {
            return;
        }
        // 与原版 ItemRenderer 一致：非方块物品一律按"精细"渲染
        boolean fancy = true;
        for (BakedModel pass : model.getRenderPasses(stack, fancy)) {
            for (RenderType type : pass.getRenderTypes(stack, fancy)) {
                VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil());
                itemRenderer.renderModelLists(pass, stack, light, overlay, pose, vc);
            }
        }
    }
}
