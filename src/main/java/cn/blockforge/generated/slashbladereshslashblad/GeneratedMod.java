package cn.blockforge.generated.slashbladereshslashblad;

import cn.blockforge.generated.slashbladereshslashblad.block.ForgeAnvilBlock;
import cn.blockforge.generated.slashbladereshslashblad.block.BladeWorkbenchBlock;
import cn.blockforge.generated.slashbladereshslashblad.block.HeatingFurnaceBlock;
import cn.blockforge.generated.slashbladereshslashblad.block.TsubaWorkbenchBlock;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.ForgeWorkbenchBlockEntity;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.BladeWorkbenchBlockEntity;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.HeatingFurnaceBlockEntity;
import cn.blockforge.generated.slashbladereshslashblad.item.FailedBladeItem;
import cn.blockforge.generated.slashbladereshslashblad.item.ForgingToolItem;
import cn.blockforge.generated.slashbladereshslashblad.item.GuideBookItem;
import cn.blockforge.generated.slashbladereshslashblad.item.HotBladeItem;
import cn.blockforge.generated.slashbladereshslashblad.item.HotFailedBladeItem;
import cn.blockforge.generated.slashbladereshslashblad.item.QuenchedBladeItem;
import cn.blockforge.generated.slashbladereshslashblad.item.RatioItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "slashbladeresh_slashblad";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /* ---------------- 木质刀条 ---------------- */
    public static final DeferredItem<Item> WOODEN_ROUGH = item("wooden_rough");
    public static final DeferredItem<Item> BAMBOO_ROUGH = item("bamboo_rough");
    // 刀条：手持右键即可与背包里的刀镡、刀柄、刀鞘现场组装（见 BladeAssembly）。
    public static final DeferredItem<Item> WOODEN_BLADE_BLANK = ITEMS.registerItem("wooden_blade_blank",
            props -> new cn.blockforge.generated.slashbladereshslashblad.item.BladeBlankItem(props, false));
    public static final DeferredItem<Item> BAMBOO_BLADE_BLANK = ITEMS.registerItem("bamboo_blade_blank",
            props -> new cn.blockforge.generated.slashbladereshslashblad.item.BladeBlankItem(props, true));
    public static final DeferredItem<Item> BLADE_HANDLE = item("blade_handle");
    public static final DeferredItem<Item> BLADE_SHEATH = item("blade_sheath");

    /* ---------------- 金属锻造链 ----------------
     * "灼热*" 这些是刚出炉、还烧红的工件：自带 1 点伤害、打中点燃目标，
     * 背包里没有钳子时每秒烫自己 1 滴血（见 item/HotMetal）。 */
    public static final DeferredItem<Item> STEEL_INGOT = item("steel_ingot");
    /* 1.0.4-r17 新增：碳粉 + 两种新钢材（低碳钢/高碳钢）。碳粉由煤炭在锻造铁砧上锤出；
     * 两种钢材在刀剑制作台上用「碳粉 + 黏土 + 钢锭」锤出，再入炉烧红参与融合钢流程。 */
    public static final DeferredItem<Item> CARBON_POWDER = item("carbon_powder");
    public static final DeferredItem<Item> LOW_CARBON_STEEL = item("low_carbon_steel");
    public static final DeferredItem<Item> HIGH_CARBON_STEEL = item("high_carbon_steel");
    public static final DeferredItem<Item> HEATED_IRON = ITEMS.registerItem("heated_iron",
            props -> new cn.blockforge.generated.slashbladereshslashblad.item.HotIngotItem(
                    props.attributes(cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));
    public static final DeferredItem<Item> HEATED_STEEL = ITEMS.registerItem("heated_steel",
            props -> new cn.blockforge.generated.slashbladereshslashblad.item.HotIngotItem(
                    props.attributes(cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));
    public static final DeferredItem<Item> HEATED_LOW_CARBON = hotIngot("heated_low_carbon");
    public static final DeferredItem<Item> HEATED_HIGH_CARBON = hotIngot("heated_high_carbon");
    public static final DeferredItem<Item> FUSED_STEEL = ratio("fused_steel");
    public static final DeferredItem<Item> HEATED_FUSED_STEEL = hotRatio("heated_fused_steel");
    public static final DeferredItem<Item> CRUDE_BLADE = ratio("crude_blade");
    public static final DeferredItem<Item> HEATED_CRUDE_BLADE = hotRatio("heated_crude_blade");
    public static final DeferredItem<Item> UNFINISHED_BLADE = ratio("unfinished_blade");
    public static final DeferredItem<Item> CLAY_BLADE = ratio("clay_blade");
    public static final DeferredItem<Item> HEATED_CLAY_BLADE = hotRatio("heated_clay_blade");
    public static final DeferredItem<HotBladeItem> HOT_BLADE = ITEMS.registerItem("hot_blade",
            props -> new HotBladeItem(props.stacksTo(1)
                    .attributes(cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));
    public static final DeferredItem<Item> QUENCHED_BLADE = ITEMS.registerItem("quenched_blade",
            props -> new QuenchedBladeItem(props.stacksTo(1)));
    // 失败的刀条：刀条类工件在烧铁炉烧好后超过 5 秒没取出，直接过火报废（见 HeatingFurnaceBlockEntity）。
    public static final DeferredItem<Item> FAILED_BLADE = ITEMS.registerItem("failed_blade", FailedBladeItem::new);
    public static final DeferredItem<HotFailedBladeItem> HOT_FAILED_BLADE = ITEMS.registerItem("hot_failed_blade",
            props -> new HotFailedBladeItem(props.stacksTo(1).attributes(
                    cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));

    /* ---------------- 刀镡 ---------------- */
    public static final DeferredItem<Item> TSUBA_WOOD_IRON = item("tsuba_wood_iron");
    public static final DeferredItem<Item> TSUBA_WOOD_GOLD = item("tsuba_wood_gold");
    public static final DeferredItem<Item> TSUBA_WOOD_COPPER = item("tsuba_wood_copper");
    public static final DeferredItem<Item> TSUBA_BAMBOO_IRON = item("tsuba_bamboo_iron");
    public static final DeferredItem<Item> TSUBA_BAMBOO_GOLD = item("tsuba_bamboo_gold");
    public static final DeferredItem<Item> TSUBA_BAMBOO_COPPER = item("tsuba_bamboo_copper");
    public static final DeferredItem<Item> TSUBA_PURE_IRON = item("tsuba_pure_iron");
    public static final DeferredItem<Item> TSUBA_PURE_GOLD = item("tsuba_pure_gold");
    public static final DeferredItem<Item> TSUBA_PURE_COPPER = item("tsuba_pure_copper");

    /* ---------------- 工具与手册 ---------------- */
    public static final DeferredItem<Item> CUTTING_KNIFE = ITEMS.registerItem("cutting_knife", ForgingToolItem::new);
    public static final DeferredItem<Item> CARVING_CHISEL = ITEMS.registerItem("carving_chisel", ForgingToolItem::new);
    public static final DeferredItem<Item> FORGING_HAMMER = ITEMS.registerItem("forging_hammer", ForgingToolItem::new);
    public static final DeferredItem<Item> TONGS = ITEMS.registerItem("tongs", ForgingToolItem::new);
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new);

    /* ---------------- 方块（noOcclusion：非立方体模型必须关掉方块遮蔽，否则相邻方块面被剔除，地面看起来“变透明”） ---------------- */
    public static final DeferredBlock<Block> TSUBA_WORKBENCH = BLOCKS.registerBlock("tsuba_workbench", TsubaWorkbenchBlock::new,
            Block.Properties.of().strength(2.5f).noOcclusion());
    public static final DeferredBlock<Block> FORGE_ANVIL = BLOCKS.registerBlock("forge_anvil", ForgeAnvilBlock::new,
            Block.Properties.of().strength(5.0f).noOcclusion());
    public static final DeferredBlock<Block> BLADE_WORKBENCH = BLOCKS.registerBlock("blade_workbench", BladeWorkbenchBlock::new,
            Block.Properties.of().strength(2.5f).noOcclusion());
    public static final DeferredBlock<Block> HEATING_FURNACE = BLOCKS.registerBlock("heating_furnace", HeatingFurnaceBlock::new,
            Block.Properties.of().strength(3.5f).noOcclusion().lightLevel(state -> 12));
    public static final DeferredItem<BlockItem> TSUBA_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(TSUBA_WORKBENCH);
    public static final DeferredItem<BlockItem> FORGE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(FORGE_ANVIL);
    public static final DeferredItem<BlockItem> BLADE_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(BLADE_WORKBENCH);
    public static final DeferredItem<BlockItem> HEATING_FURNACE_ITEM = ITEMS.registerSimpleBlockItem(HEATING_FURNACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeWorkbenchBlockEntity>> FORGE_WORKBENCH_ENTITY = BLOCK_ENTITY_TYPES.register(
            "forge_workbench", () -> BlockEntityType.Builder.of(ForgeWorkbenchBlockEntity::create,
                    TSUBA_WORKBENCH.get(), FORGE_ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BladeWorkbenchBlockEntity>> BLADE_WORKBENCH_ENTITY = BLOCK_ENTITY_TYPES.register(
            "blade_workbench", () -> BlockEntityType.Builder.of(BladeWorkbenchBlockEntity::create,
                    BLADE_WORKBENCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatingFurnaceBlockEntity>> HEATING_FURNACE_ENTITY = BLOCK_ENTITY_TYPES.register(
            "heating_furnace", () -> BlockEntityType.Builder.of(HeatingFurnaceBlockEntity::create,
                    HEATING_FURNACE.get()).build(null));

    @SuppressWarnings("unchecked")
    private static <T extends Item> DeferredItem<T> item(String id) {
        return (DeferredItem<T>) ITEMS.registerSimpleItem(id, new Item.Properties());
    }

    /** 带铁/钢配比的工件：配比写在 NBT 上，必须不可堆叠，避免两批材料混标。 */
    @SuppressWarnings("unchecked")
    private static <T extends Item> DeferredItem<T> ratio(String id) {
        return (DeferredItem<T>) ITEMS.registerItem(id, props -> new RatioItem(props.stacksTo(1)));
    }

    /** 带配比、而且烧红的工件（灼热融合钢 / 灼热粗制刀条 / 灼热覆土刀条）。 */
    @SuppressWarnings("unchecked")
    private static <T extends Item> DeferredItem<T> hotRatio(String id) {
        return (DeferredItem<T>) ITEMS.registerItem(id,
                props -> new cn.blockforge.generated.slashbladereshslashblad.item.HotRatioItem(
                        props.stacksTo(1)
                                .attributes(cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));
    }

    /** 烧红的钢锭（灼热低碳钢 / 灼热高碳钢）：行为与灼热钢锭一致。 */
    @SuppressWarnings("unchecked")
    private static <T extends Item> DeferredItem<T> hotIngot(String id) {
        return (DeferredItem<T>) ITEMS.registerItem(id,
                props -> new cn.blockforge.generated.slashbladereshslashblad.item.HotIngotItem(
                        props.attributes(cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.attackModifier())));
    }

    /** 创造模式专用物品栏：收录本模组全部物品与方块，按工序排序。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FORGE_TAB = CREATIVE_MODE_TABS.register("forge_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(FORGING_HAMMER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(TSUBA_WORKBENCH_ITEM.get());
                        output.accept(BLADE_WORKBENCH_ITEM.get());
                        output.accept(FORGE_ANVIL_ITEM.get());
                        output.accept(HEATING_FURNACE_ITEM.get());
                        output.accept(CUTTING_KNIFE.get());
                        output.accept(CARVING_CHISEL.get());
                        output.accept(FORGING_HAMMER.get());
                        output.accept(TONGS.get());
                        output.accept(HEATED_IRON.get());
                        output.accept(HEATED_STEEL.get());
                        output.accept(STEEL_INGOT.get());
                        output.accept(CARBON_POWDER.get());
                        output.accept(LOW_CARBON_STEEL.get());
                        output.accept(HIGH_CARBON_STEEL.get());
                        output.accept(HEATED_LOW_CARBON.get());
                        output.accept(HEATED_HIGH_CARBON.get());
                        output.accept(FUSED_STEEL.get());
                        output.accept(HEATED_FUSED_STEEL.get());
                        output.accept(CRUDE_BLADE.get());
                        output.accept(HEATED_CRUDE_BLADE.get());
                        output.accept(UNFINISHED_BLADE.get());
                        output.accept(CLAY_BLADE.get());
                        output.accept(HEATED_CLAY_BLADE.get());
                        output.accept(HOT_BLADE.get());
                        output.accept(QUENCHED_BLADE.get());
                        output.accept(FAILED_BLADE.get());
                        output.accept(HOT_FAILED_BLADE.get());
                        output.accept(WOODEN_ROUGH.get());
                        output.accept(BAMBOO_ROUGH.get());
                        output.accept(WOODEN_BLADE_BLANK.get());
                        output.accept(BAMBOO_BLADE_BLANK.get());
                        output.accept(TSUBA_WOOD_IRON.get());
                        output.accept(TSUBA_WOOD_GOLD.get());
                        output.accept(TSUBA_WOOD_COPPER.get());
                        output.accept(TSUBA_BAMBOO_IRON.get());
                        output.accept(TSUBA_BAMBOO_GOLD.get());
                        output.accept(TSUBA_BAMBOO_COPPER.get());
                        output.accept(TSUBA_PURE_IRON.get());
                        output.accept(TSUBA_PURE_GOLD.get());
                        output.accept(TSUBA_PURE_COPPER.get());
                        output.accept(BLADE_HANDLE.get());
                        output.accept(BLADE_SHEATH.get());
                        // 直接放"写好内容"的那一本，从创造栏拿出来的手册可以正常翻阅。
                        output.accept(GuideBookItem.written(GUIDE_BOOK.get()));
                    })
                    .build());

    public GeneratedMod(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        // 检视沿用网站版机制：向重锋的连招注册表登记带拔刀的 inspect_a
        // 一个 ComboState，并注册那五个原版同款音效；播放全交给重锋自己做。
        InspectComboStates.COMBO_STATE.register(modBus);
        InspectSounds.SOUNDS.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            UltimateJudgementCutNetwork.register(event);
            BladeInspectNetwork.register(event);
            BladeAssemblyNetwork.register(event);
        });
        NeoForge.EVENT_BUS.register(PlayerBookEvents.class);
        NeoForge.EVENT_BUS.register(ForgeEvents.class);
        NeoForge.EVENT_BUS.register(UltimateJudgementCut.class);
        NeoForge.EVENT_BUS.register(InspectInterruptGuard.class);
        NeoForge.EVENT_BUS.register(BladeAssembly.class);
    }
}
