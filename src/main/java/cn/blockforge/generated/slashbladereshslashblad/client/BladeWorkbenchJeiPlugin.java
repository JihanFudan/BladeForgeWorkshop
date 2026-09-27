package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladeWorkbenchRecipes;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.RecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 实际制作与 JEI 展示共用材料清单，红玉使用本体的名刀数据。 */
@JeiPlugin
public final class BladeWorkbenchJeiPlugin implements mezz.jei.api.IModPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            GeneratedMod.MOD_ID, "blade_workbench");
    public static final RecipeType<BladeWorkbenchRecipes.NamedRecipe> TYPE = RecipeType.create(
            GeneratedMod.MOD_ID, "blade_workbench", BladeWorkbenchRecipes.NamedRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, BladeWorkbenchRecipes.namedRecipes(Minecraft.getInstance().level));
        for (var blade : List.of(GeneratedMod.WOODEN_ROUGH.get(), GeneratedMod.BAMBOO_ROUGH.get(),
                GeneratedMod.WOODEN_BLADE_BLANK.get(), GeneratedMod.BAMBOO_BLADE_BLANK.get())) {
            registration.addIngredientInfo(blade, Component.literal(
                    "双手加工：一手拿切割刀，另一只手放木板或竹板（主副不限），不蹲下朝空中右键，得到对应刀条雏形。把雏形拿在一只手里，另一只手换雕刻凿再右键，得到木刀条或竹刀条。竹板走竹路线，木板走木路线。"));
        }
        for (var blank : List.of(GeneratedMod.WOODEN_BLADE_BLANK.get(), GeneratedMod.BAMBOO_BLADE_BLANK.get())) {
            registration.addIngredientInfo(blank, Component.literal(
                    "现场组装：手持该刀条对空气右键，背包里备齐对应刀镡、刀柄、刀鞘各一件时，播放约 4 秒组装动画（刀镡上刀、刀柄安装、插入刀鞘）并直接得到木偶或竹光；材料不齐会提示背包内没有完整合成材料。木刀条配木铁/木金/铜木刀镡，竹刀条配竹铁/竹金/铜竹刀镡；几种都有时按金＞铜＞铁的顺序取用。"));
        }
        for (var part : List.of(GeneratedMod.TSUBA_WOOD_IRON.get(), GeneratedMod.TSUBA_WOOD_GOLD.get(),
                GeneratedMod.TSUBA_WOOD_COPPER.get(),
                GeneratedMod.TSUBA_BAMBOO_IRON.get(), GeneratedMod.TSUBA_BAMBOO_GOLD.get(),
                GeneratedMod.TSUBA_BAMBOO_COPPER.get(),
                GeneratedMod.BLADE_HANDLE.get(), GeneratedMod.BLADE_SHEATH.get())) {
            registration.addIngredientInfo(part, Component.literal(
                    "刀镡制作台：空台放一块木板或一块竹板，用切割刀右键，再用雕刻凿右键，放一枚铁锭、金锭或铜锭，再用雕刻凿右键。空手右键一次取出刀镡、刀柄和刀鞘。木板制木质刀镡，竹板制竹质刀镡；铁锭、金锭或铜锭决定嵌入材质。"));
        }
        for (var id : List.of(cn.blockforge.generated.slashbladereshslashblad.BladeData.SLASHBLADE_WOOD,
                cn.blockforge.generated.slashbladereshslashblad.BladeData.SLASHBLADE_BAMBOO)) {
            ItemStack blade = cn.blockforge.generated.slashbladereshslashblad.BladeData.sword(id);
            if (!blade.isEmpty()) registration.addIngredientInfo(blade.getItem(), Component.literal(
                    "不再需要刀剑制作台：手持木刀条或竹刀条右键，背包里有对应刀镡、刀柄、刀鞘各一件即可播放组装动画完成制作。铁刀镡耐久更高，金刀镡攻击更高，铜刀镡攻击耐久各加一。"));
        }
        registration.addIngredientInfo(GeneratedMod.STEEL_INGOT.get(), Component.literal(
                "锻造铁砧：在空铁砧上只放一枚灼热铁锭，用锻造锤右键，得到一枚钢锭。灼热铁锭由铁锭在烧铁炉中加热获得。钢锭也是低碳钢、高碳钢的原料；参与融合时，一枚钢锭的基数是攻＋２、耐＋２。"));
        registration.addIngredientInfo(GeneratedMod.CARBON_POWDER.get(), Component.literal(
                "锻造铁砧：手持煤炭或木炭右键空铁砧放上一块，再用锻造锤右键研磨，得到 4 份碳粉。碳粉是低碳钢、高碳钢的原料，本身不能烧红。"));
        registration.addIngredientInfo(GeneratedMod.LOW_CARBON_STEEL.get(), Component.literal(
                "刀剑制作台：台面逐件右键放入 钢锭×1、黏土球×1、碳粉×1，再用锻造锤右键锻打，得到低碳钢。放进烧铁炉烧成灼热低碳钢，与其余灼热钢混满 5 枚可融成融合钢（低碳钢单枚基数：攻＋１，耐＋３）。"));
        registration.addIngredientInfo(GeneratedMod.HIGH_CARBON_STEEL.get(), Component.literal(
                "刀剑制作台：台面逐件右键放入 钢锭×1、黏土球×1、碳粉×2，再用锻造锤右键锻打，得到高碳钢。放进烧铁炉烧成灼热高碳钢，与其余灼热钢混满 5 枚可融成融合钢（高碳钢单枚基数：攻＋３，耐＋１）。"));
        for (var material : List.of(GeneratedMod.HEATED_IRON.get(), GeneratedMod.HEATED_STEEL.get(),
                GeneratedMod.HEATED_LOW_CARBON.get(), GeneratedMod.HEATED_HIGH_CARBON.get(),
                GeneratedMod.HEATED_FUSED_STEEL.get(), GeneratedMod.HEATED_CRUDE_BLADE.get(),
                GeneratedMod.HEATED_CLAY_BLADE.get())) {
            registration.addIngredientInfo(material, Component.literal(
                    "烧铁炉：铁锭、钢锭、低碳钢、高碳钢、融合钢、粗制刀条、覆土刀条分别加热为对应灼热材料。手持材料右键空炉放入，约8秒后用钳子右键取出，无需另添燃料。锭材每批最多5枚，取完后再放下一批；带钢材记录的刀条工件逐件加热。"));
        }
        registration.addIngredientInfo(GeneratedMod.FUSED_STEEL.get(), Component.literal(
                "锻造铁砧第一锤：灼热钢锭、灼热低碳钢、灼热高碳钢随意混放，凑满共 5 枚（每次右键放 1 枚），放齐后用锻造锤右键得到融合钢。每种钢的单枚基数：钢 攻＋２耐＋２、低碳钢 攻＋１耐＋３、高碳钢 攻＋３耐＋１；融合钢按各钢枚数加权平均、四舍五入，例如 钢×２＋高碳钢×３ → 攻＋３耐＋１。这份钢材记录沿后续工序保留到成品刀条。"));
        registration.addIngredientInfo(GeneratedMod.CRUDE_BLADE.get(), Component.literal(
                "锻造铁砧第二锤：融合钢放进烧铁炉加热，用钳子取出灼热融合钢，右键放上空铁砧，再用锻造锤右键，得到粗制刀条。"));
        registration.addIngredientInfo(GeneratedMod.UNFINISHED_BLADE.get(), Component.literal(
                "锻造铁砧第三锤：粗制刀条入炉烧红，用钳子取出灼热粗制刀条，放上空铁砧，用锻造锤右键得到未完成的刀条。"));
        registration.addIngredientInfo(GeneratedMod.CLAY_BLADE.get(), Component.literal(
                "覆土：主手拿未完成的刀条，副手拿一个黏土球，右键空烧铁炉，消耗一团黏土，得到覆土刀条并直接入炉加热。约8秒后用钳子取出灼热覆土刀条。"));
        registration.addIngredientInfo(GeneratedMod.HOT_BLADE.get(), Component.literal(
                "最后一锤：灼热覆土刀条放上空锻造铁砧，用锻造锤右键得到烫手的刀条。右键有水炼药锅可立即淬火，也可放在背包中，从最后一锤起等待约30秒自然冷却为成品刀条。"));
        registration.addIngredientInfo(GeneratedMod.QUENCHED_BLADE.get(), Component.literal(
                "成品刀条：五枚灼热钢（钢、低碳钢、高碳钢可混放）上锻造铁砧锤成融合钢，属性按各钢枚数加权分配；烧红再锤成粗制刀条；再烧红锤成未完成刀条。另一只手拿黏土，右键烧铁炉覆土并加热，用钳子取出后上砧锤成烫手刀条。右键有水炼药锅淬火或冷却30秒。"));
        for (var guard : List.of(GeneratedMod.TSUBA_PURE_IRON.get(), GeneratedMod.TSUBA_PURE_GOLD.get(),
                GeneratedMod.TSUBA_PURE_COPPER.get())) {
            registration.addIngredientInfo(guard, Component.literal(
                    "刀镡制作台：手持一枚铁锭、金锭或铜锭右键空台，再用雕刻凿右键加工，空手右键取出纯金属刀镡。除木偶与竹光外的所有拔刀剑不接受木质、竹质刀镡，只接受纯铁、纯金或纯铜刀镡。纯铁耐久＋５，纯金攻击＋２耐久＋１，纯铜攻击＋１耐久＋１。"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(GeneratedMod.BLADE_WORKBENCH_ITEM.get(), TYPE);
        registration.addRecipeCatalyst(GeneratedMod.FORGING_HAMMER.get(), TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING,
                runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                        .filter(holder -> BladeWorkbenchRecipes.shouldMove(holder.value(), level.registryAccess())).toList());
        runtime.getRecipeManager().hideRecipes(RecipeTypes.SMITHING,
                runtime.getRecipeManager().createRecipeLookup(RecipeTypes.SMITHING).get()
                        .filter(holder -> BladeWorkbenchRecipes.shouldMove(holder.value(), level.registryAccess())).toList());
    }

    private static final class Category extends AbstractRecipeCategory<BladeWorkbenchRecipes.NamedRecipe> {
        private Category(IGuiHelper gui) {
            super(TYPE, Component.literal("刀剑制作台 · 名刀锻造"),
                    gui.createDrawableItemStack(GeneratedMod.BLADE_WORKBENCH_ITEM.get().getDefaultInstance()),
                    180, 150);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BladeWorkbenchRecipes.NamedRecipe recipe, IFocusGroup focuses) {
            List<BladeWorkbenchRecipes.Requirement> requirements = recipe.requirements();
            for (int i = 0; i < requirements.size(); i++) {
                var requirement = requirements.get(i);
                builder.addInputSlot(4 + (i % 4) * 25, 18 + (i / 4) * 25)
                        .setStandardSlotBackground()
                        .addItemStacks(requirement.displayStacks());
            }
            builder.addSlot(RecipeIngredientRole.CATALYST, 113, 43)
                    .setStandardSlotBackground()
                    .addItemStack(GeneratedMod.FORGING_HAMMER.get().getDefaultInstance());
            builder.addOutputSlot(155, 43).setOutputSlotBackground()
                    .addItemStack(recipe.output(Minecraft.getInstance().level));
        }

        @Override
        public void draw(BladeWorkbenchRecipes.NamedRecipe recipe, IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, font.plainSubstrByWidth(recipe.displayName(), 172), 3, 2, 0x333333, false);
            graphics.drawString(font, "锻造锤", 107, 28, 0x333333, false);
            graphics.drawString(font, "→", 136, 47, 0x333333, false);
            graphics.drawString(font, "右键逐件放入；空手取回", 3, 116, 0x333333, false);
            graphics.drawString(font, "锻造锤完成；沿用原刀条件", 3, 129, 0x333333, false);
            graphics.drawString(font, "纯金属刀镡三选一；锤不消耗", 3, 142, 0x555555, false);
        }
    }
}
