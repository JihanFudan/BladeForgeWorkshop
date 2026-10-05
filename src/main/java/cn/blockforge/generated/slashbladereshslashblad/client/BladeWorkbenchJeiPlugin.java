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
        registration.addIngredientInfo(GeneratedMod.GUIDE_BOOK.get(), Component.literal(
                "《锻刀工坊游玩手册》：按工序讲解本模组全部玩法，拿在手里右键阅读。初次进入世界发放一本，也可用书与锻造锤合成。"));
        registration.addIngredientInfo(GeneratedMod.FORGING_HAMMER.get(), Component.literal(
                "锻造锤：各工位与锻造铁砧都靠右键推进工序，不会被消耗；与一本书可合成《锻刀工坊游玩手册》。"));
        for (var blade : List.of(GeneratedMod.WOODEN_ROUGH.get(), GeneratedMod.BAMBOO_ROUGH.get(),
                GeneratedMod.WOODEN_BLADE_BLANK.get(), GeneratedMod.BAMBOO_BLADE_BLANK.get())) {
            registration.addIngredientInfo(blade, Component.literal(
                    "双手加工：一手拿切割刀，另一只手放木板或竹板（主副不限），朝空中右键得到刀条雏形；雏形配雕刻凿再右键，得到木刀条或竹刀条。"));
        }
        for (var blank : List.of(GeneratedMod.WOODEN_BLADE_BLANK.get(), GeneratedMod.BAMBOO_BLADE_BLANK.get())) {
            registration.addIngredientInfo(blank, Component.literal(
                    "现场组装：手持该刀条对空气右键，背包里备齐对应刀镡、刀柄、刀鞘各一件时，播约 4 秒组装动画，直接得到木偶或竹光。木刀条配木铁/木金/铜木刀镡，竹刀条配竹铁/竹金/铜竹刀镡；几种都有时按金＞铜＞铁取用。"));
        }
        for (var part : List.of(GeneratedMod.TSUBA_WOOD_IRON.get(), GeneratedMod.TSUBA_WOOD_GOLD.get(),
                GeneratedMod.TSUBA_WOOD_COPPER.get(),
                GeneratedMod.TSUBA_BAMBOO_IRON.get(), GeneratedMod.TSUBA_BAMBOO_GOLD.get(),
                GeneratedMod.TSUBA_BAMBOO_COPPER.get(),
                GeneratedMod.BLADE_HANDLE.get(), GeneratedMod.BLADE_SHEATH.get())) {
            registration.addIngredientInfo(part, Component.literal(
                    "刀镡制作台：空台放一块木板或竹板，切割刀右键、雕刻凿右键、放一枚铁/金/铜锭、再用雕刻凿右键，空手右键一次取出刀镡、刀柄和刀鞘。"));
        }
        for (var id : List.of(cn.blockforge.generated.slashbladereshslashblad.BladeData.SLASHBLADE_WOOD,
                cn.blockforge.generated.slashbladereshslashblad.BladeData.SLASHBLADE_BAMBOO)) {
            ItemStack blade = cn.blockforge.generated.slashbladereshslashblad.BladeData.sword(id);
            if (!blade.isEmpty()) registration.addIngredientInfo(blade.getItem(), Component.literal(
                    "手持木刀条或竹刀条右键，背包里有对应刀镡、刀柄、刀鞘各一件即可组装成刀。铁镡耐久高，金镡攻击高，铜镡攻耐各加一。"));
        }
        registration.addIngredientInfo(GeneratedMod.STEEL_INGOT.get(), Component.literal(
                "锻造铁砧：空铁砧放一枚灼热铁锭，锻造锤右键炼成钢锭。钢锭也是低碳钢、高碳钢的原料；参与融合时基数为攻＋２、耐＋２。"));
        registration.addIngredientInfo(GeneratedMod.CARBON_POWDER.get(), Component.literal(
                "锻造铁砧：煤炭或木炭右键空铁砧放上一块，锻造锤右键研磨得 4 份碳粉。碳粉是低碳钢、高碳钢的原料，本身不能烧红。"));
        registration.addIngredientInfo(GeneratedMod.LOW_CARBON_STEEL.get(), Component.literal(
                "刀剑制作台：台面逐件右键放入 钢锭×1、黏土球×1、碳粉×1，锻造锤右键锻打得低碳钢（基数：攻＋１耐＋３），入炉烧红后可参与融合。"));
        registration.addIngredientInfo(GeneratedMod.HIGH_CARBON_STEEL.get(), Component.literal(
                "刀剑制作台：台面逐件右键放入 钢锭×1、黏土球×1、碳粉×2，锻造锤右键锻打得上高碳钢（基数：攻＋３耐＋１），入炉烧红后可参与融合。"));
        for (var material : List.of(GeneratedMod.HEATED_IRON.get(), GeneratedMod.HEATED_STEEL.get(),
                GeneratedMod.HEATED_LOW_CARBON.get(), GeneratedMod.HEATED_HIGH_CARBON.get(),
                GeneratedMod.HEATED_FUSED_STEEL.get(), GeneratedMod.HEATED_CRUDE_BLADE.get(),
                GeneratedMod.HEATED_CLAY_BLADE.get())) {
            registration.addIngredientInfo(material, Component.literal(
                    "烧铁炉：铁锭、钢锭、低碳钢、高碳钢、融合钢、粗制刀条、覆土刀条分别加热为对应灼热材料。手持材料右键空炉放入，约8秒后用钳子取出，无需另添燃料。每批最多5枚，取完再放下批。刀条烧红后 5 秒内必须夹出，否则颜色渐变成橙黄并过火，出炉为灼热的失败的刀条。"));
        }
        registration.addIngredientInfo(GeneratedMod.HOT_FAILED_BLADE.get(), Component.literal(
                "灼热的失败的刀条：刀条在烧铁炉里过火了。空手拿着会烫手，自然冷却 15 秒或右键水炼药锅降温，得到失败的刀条。"));
        registration.addIngredientInfo(GeneratedMod.FAILED_BLADE.get(), Component.literal(
                "失败的刀条：放进熔炉或烧铁炉回烧，每枚固定烧成 3 枚铁锭。"));
        registration.addIngredientInfo(GeneratedMod.FUSED_STEEL.get(), Component.literal(
                "锻造铁砧第一锤：灼热钢锭、灼热低碳钢、灼热高碳钢随意混放凑满 5 枚（每次右键放 1 枚），锻造锤右键得融合钢。单枚基数：钢 攻＋２耐＋２、低碳钢 攻＋１耐＋３、高碳钢 攻＋３耐＋１；融合钢按枚数加权平均、四舍五入，钢材记录沿后续工序保留到成品刀条。"));
        registration.addIngredientInfo(GeneratedMod.CRUDE_BLADE.get(), Component.literal(
                "锻造铁砧第二锤：融合钢放进烧铁炉加热，用钳子取出灼热融合钢，右键放上空铁砧，再用锻造锤右键，得到粗制刀条。"));
        registration.addIngredientInfo(GeneratedMod.UNFINISHED_BLADE.get(), Component.literal(
                "锻造铁砧第三锤：粗制刀条入炉烧红，用钳子取出灼热粗制刀条，放上空铁砧，用锻造锤右键得到未完成的刀条。"));
        registration.addIngredientInfo(GeneratedMod.CLAY_BLADE.get(), Component.literal(
                "覆土：主手拿未完成的刀条，副手拿一个黏土球，右键空烧铁炉，消耗一团黏土，得到覆土刀条并直接入炉加热。约8秒后用钳子取出灼热覆土刀条。"));
        registration.addIngredientInfo(GeneratedMod.HOT_BLADE.get(), Component.literal(
                "最后一锤：灼热覆土刀条放上空锻造铁砧，锻造锤右键得烫手的刀条。右键水炼药锅立即淬火，或带在包里约30秒自然冷却为成品刀条。"));
        registration.addIngredientInfo(GeneratedMod.QUENCHED_BLADE.get(), Component.literal(
                "成品刀条：淬火或自然冷却得到。除名刀配方外，手持它对空气右键，备齐纯金属刀镡、刀柄、刀鞘、雪块各一件，可组装名刀·寒霜（点击寒霜本体也能看到组装说明）。"));
        // 寒霜的信息栏：与木偶/竹光同样的写法，绑定在寒霜本体的物品栈上，
        // 在 JEI 里点击寒霜就能看到这一栏文字说明。
        var jeiLevel = Minecraft.getInstance().level;
        if (jeiLevel != null) {
            ItemStack frostStack = cn.blockforge.generated.slashbladereshslashblad.BladeData
                    .frostBlade(jeiLevel.registryAccess());
            if (!frostStack.isEmpty()) {
                registration.addItemStackInfo(frostStack, Component.literal(
                        "现场组装：手持成品刀条对空气右键，背包里备齐纯金属刀镡、刀柄、刀鞘、雪块各一件时，"
                                + "播约 4 秒组装动画，直接得到名刀·寒霜。"));
            }
        }
        for (var guard : List.of(GeneratedMod.TSUBA_PURE_IRON.get(), GeneratedMod.TSUBA_PURE_GOLD.get(),
                GeneratedMod.TSUBA_PURE_COPPER.get())) {
            registration.addIngredientInfo(guard, Component.literal(
                    "刀镡制作台：铁/金/铜锭右键空台，雕刻凿右键加工，空手取出纯金属刀镡。除木偶与竹光外的拔刀剑只接受纯金属刀镡。纯铁耐＋５，纯金攻＋２耐＋１，纯铜攻＋１耐＋１。"));
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
            graphics.drawString(font, "纯金属刀镡三选一", 3, 142, 0x555555, false);
        }
    }
}
