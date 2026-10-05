package cn.blockforge.generated.slashbladereshslashblad.blockentity;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.BladeWorkbenchRecipes;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

/** 无序放入；制作时重组原配方的有序网格，再检查附属自身的刀剑条件。 */
public class BladeWorkbenchBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 18;
    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT);
    public BladeWorkbenchBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public static BladeWorkbenchBlockEntity create(BlockPos pos, BlockState state) {
        return new BladeWorkbenchBlockEntity(GeneratedMod.BLADE_WORKBENCH_ENTITY.get(), pos, state);
    }
    public SimpleContainer items() { return inventory; }
    public void dropStoredItems(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, inventory);
    }
    public ItemInteractionResult interactBlade(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (held.isEmpty()) {
            if (stacks().stream().anyMatch(s -> !s.isEmpty())) {
                for (int i = 0; i < SLOT_COUNT; i++) giveBack(player, inventory.removeItemNoUpdate(i));
                tell(player, "台上的材料已全部收回背包。"); sync();
            } else tell(player, "按 JEI 清单逐件放入，锻造锤右键完成，空手可取回。");
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(GeneratedMod.FORGING_HAMMER.get())) {
            forge(level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }
        int slot = -1;
        for (int i = 0; i < SLOT_COUNT; i++) if (inventory.getItem(i).isEmpty()) { slot = i; break; }
        if (slot < 0) { tell(player, "台面材料已满，空手右键可全部取回。"); return ItemInteractionResult.SUCCESS; }
        // 合金原料（碳粉/黏土/钢锭）：只要台面还空着或全是同类原料，就直接摆上去。
        if (isAlloyMaterial(held)) {
            boolean tableAllAlloy = stacks().stream().allMatch(s -> s.isEmpty() || isAlloyMaterial(s));
            if (!tableAllAlloy) {
                tell(player, "台面已有材料，先空手取回，再配碳粉、黏土和钢锭。");
                return ItemInteractionResult.SUCCESS;
            }
            inventory.setItem(slot, held.copyWithCount(1)); held.shrink(1);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 1.1f);
            tell(player, "原料已放齐：钢锭1＋黏土1＋碳粉1＝低碳钢，再加一份碳粉＝高碳钢。");
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        List<ItemStack> proposed = stacks();
        proposed.set(slot, held.copyWithCount(1));
        List<BladeWorkbenchRecipes.NamedRecipe> recipes = BladeWorkbenchRecipes.namedRecipes(level);
        if (recipes.stream().noneMatch(r -> r.acceptsPartial(proposed))) {
            tell(player, "材料与配方不符；空手右键可以取回。");
            return ItemInteractionResult.SUCCESS;
        }
        inventory.setItem(slot, held.copyWithCount(1)); held.shrink(1);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 1.1f);
        var complete = BladeWorkbenchRecipes.find(stacks(), level);
        tell(player, complete == null ? "已放入一件；材料与原刀条件都须按 JEI 达标。"
                : complete.displayName() + "材料与条件齐全，手持锻造锤右键完成制作。");
        sync();
        return ItemInteractionResult.SUCCESS;
    }
    private List<ItemStack> stacks() {
        List<ItemStack> result = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) result.add(inventory.getItem(i));
        return result;
    }
    private void forge(Level level, BlockPos pos, Player player) {
        List<ItemStack> inputs = stacks();
        // 先判合金：钢锭 + 黏土 + 碳粉（1 份低碳、2 份高碳），与其它名刀配方互斥。
        if (inputs.stream().anyMatch(this::isAlloyMaterial)) {
            forgeAlloy(level, pos, player, inputs);
            return;
        }
        var recipe = BladeWorkbenchRecipes.find(inputs, level);
        if (recipe == null) { tell(player, "材料或原刀条件未满足，请按 JEI 检查。"); return; }
        ItemStack out = recipe.assemble(inputs, level);
        if (out.isEmpty() || !BladeData.isSlashBlade(out)) { tell(player, "配方未生成有效拔刀剑，材料未消耗。"); return; }
        // 所有运算在产物副本上完成；成功后才消耗材料，避免失败丢刀。
        out = out.copy();
        ItemStack tsuba = stacks().stream().filter(this::isTsuba).findFirst().orElse(ItemStack.EMPTY);
        boolean gold = tsuba.is(GeneratedMod.TSUBA_WOOD_GOLD.get())
                || tsuba.is(GeneratedMod.TSUBA_BAMBOO_GOLD.get())
                || tsuba.is(GeneratedMod.TSUBA_PURE_GOLD.get());
        boolean copper = !gold && (tsuba.is(GeneratedMod.TSUBA_WOOD_COPPER.get())
                || tsuba.is(GeneratedMod.TSUBA_BAMBOO_COPPER.get())
                || tsuba.is(GeneratedMod.TSUBA_PURE_COPPER.get()));
        String guard = gold ? "gold" : copper ? "copper" : "iron";
        ItemStack blank = find(GeneratedMod.QUENCHED_BLADE.get());
        // 刀镡修正：金 攻+2 耐+1，铜 攻+1 耐+1，铁 攻+0 耐+5。
        int attack = (gold ? 2 : copper ? 1 : 0) + BladeData.ratioAttackBonus(blank);
        int durability = (gold || copper ? 1 : 5) + BladeData.ratioDurabilityBonus(blank);
        ItemStack source = inputs.stream().filter(BladeData::isSlashBlade).findFirst().orElse(ItemStack.EMPTY);
        ItemStack definition = recipe.output(level);
        if (recipe.basic()) {
            BladeData.markCompatibleBlade(out, attack, durability, guard,
                    out.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(BladeData.SLASHBLADE_BAMBOO)));
        } else {
            if (!source.isEmpty()) BladeData.inheritProgress(source, out, definition);
            BladeData.applyUpgradeBonus(out, attack, durability, guard);
        }
        BladeData.rememberDefinition(out, definition);
        List<ItemStack> remains = recipe.remainders(inputs, level);
        for (int i = 0; i < SLOT_COUNT; i++) inventory.removeItemNoUpdate(i);
        giveBack(player, out);
        for (ItemStack remainder : remains) giveBack(player, remainder);
        tell(player, recipe.displayName() + "锻造完成！原刀的杀敌、耀魂、精炼与攻击养成已继承。");
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.75f);
        level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 1.1f);
        sync();
    }
    private ItemStack find(net.minecraft.world.item.Item item) {
        return stacks().stream().filter(s -> s.is(item)).findFirst().orElse(ItemStack.EMPTY);
    }
    /** 合金原料：碳粉、黏土球、钢锭。 */
    private boolean isAlloyMaterial(ItemStack stack) {
        return stack.is(GeneratedMod.CARBON_POWDER.get())
                || stack.is(net.minecraft.world.item.Items.CLAY_BALL)
                || stack.is(GeneratedMod.STEEL_INGOT.get());
    }
    /** 钢锭×1 + 黏土×1 + 碳粉×1 → 低碳钢；碳粉×2 → 高碳钢。台面只允许这三类原料。 */
    private void forgeAlloy(Level level, BlockPos pos, Player player, List<ItemStack> inputs) {
        int carbon = 0, clay = 0, steel = 0;
        for (ItemStack s : inputs) {
            if (s.isEmpty()) continue;
            if (s.is(GeneratedMod.CARBON_POWDER.get())) carbon += s.getCount();
            else if (s.is(net.minecraft.world.item.Items.CLAY_BALL)) clay += s.getCount();
            else if (s.is(GeneratedMod.STEEL_INGOT.get())) steel += s.getCount();
            else { tell(player, "合金台面只能放碳粉、黏土和钢锭，先空手右键取回多余材料。"); return; }
        }
        net.minecraft.world.item.Item result = null;
        if (steel == 1 && clay == 1 && carbon == 1) result = GeneratedMod.LOW_CARBON_STEEL.get();
        else if (steel == 1 && clay == 1 && carbon == 2) result = GeneratedMod.HIGH_CARBON_STEEL.get();
        if (result == null) {
            tell(player, "配比不对：低碳钢＝钢锭1＋黏土1＋碳粉1；高碳钢＝再加一份碳粉。当前碳 "
                    + carbon + "、黏土 " + clay + "、钢锭 " + steel + "。");
            return;
        }
        for (int i = 0; i < SLOT_COUNT; i++) inventory.removeItemNoUpdate(i);
        giveBack(player, new ItemStack(result));
        tell(player, "锻打完成，得到" + (result == GeneratedMod.LOW_CARBON_STEEL.get() ? "低碳钢" : "高碳钢")
                + "！放进烧铁炉烧红，再凑满 5 枚灼热钢（可混钢种）上砧融成融合钢。");
        level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.9f, 1.05f);
        sync();
    }
    private boolean isTsuba(ItemStack stack) {
        return stack.is(GeneratedMod.TSUBA_WOOD_IRON.get()) || stack.is(GeneratedMod.TSUBA_WOOD_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_WOOD_COPPER.get())
                || stack.is(GeneratedMod.TSUBA_BAMBOO_IRON.get()) || stack.is(GeneratedMod.TSUBA_BAMBOO_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_BAMBOO_COPPER.get())
                || stack.is(GeneratedMod.TSUBA_PURE_IRON.get()) || stack.is(GeneratedMod.TSUBA_PURE_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_PURE_COPPER.get());
    }
    private static void tell(Player player, String text) { player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true); }
    private static void giveBack(Player player, ItemStack stack) { if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack); }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag, registries); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < SLOT_COUNT; i++) inventory.setItem(i, ItemStack.parseOptional(registries, tag.getCompound("Slot" + i)));
    }
    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < SLOT_COUNT; i++) tag.put("Slot" + i, inventory.getItem(i).saveOptional(registries));
    }
}
