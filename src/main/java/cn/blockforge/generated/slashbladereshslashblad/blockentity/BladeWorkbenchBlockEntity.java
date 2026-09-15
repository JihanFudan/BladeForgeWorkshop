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
            } else tell(player, "按 JEI 的刀剑制作台配方逐件放入材料，最后用锻造锤右键；空手右键取回材料。");
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(GeneratedMod.FORGING_HAMMER.get())) {
            forge(level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }
        int slot = -1;
        for (int i = 0; i < SLOT_COUNT; i++) if (inventory.getItem(i).isEmpty()) { slot = i; break; }
        if (slot < 0) { tell(player, "台面材料已满，空手右键可全部取回。"); return ItemInteractionResult.SUCCESS; }
        List<ItemStack> proposed = stacks();
        proposed.set(slot, held.copyWithCount(1));
        List<BladeWorkbenchRecipes.NamedRecipe> recipes = BladeWorkbenchRecipes.namedRecipes(level);
        if (recipes.stream().noneMatch(r -> r.acceptsPartial(proposed))) {
            tell(player, "材料已放够或不属于同一配方，请按 JEI 清单放置；空手右键可以取回。");
            return ItemInteractionResult.SUCCESS;
        }
        inventory.setItem(slot, held.copyWithCount(1)); held.shrink(1);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 1.1f);
        var complete = BladeWorkbenchRecipes.find(stacks(), level);
        tell(player, complete == null ? "已放入一件，继续按 JEI 补齐材料；原刀的杀敌、耀魂等条件也须达标。"
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
        var recipe = BladeWorkbenchRecipes.find(inputs, level);
        if (recipe == null) { tell(player, "材料数量或原刀条件未满足，请按 JEI 配方检查。"); return; }
        ItemStack out = recipe.assemble(inputs, level);
        if (out.isEmpty() || !BladeData.isSlashBlade(out)) { tell(player, "配方未生成有效拔刀剑，材料未消耗。"); return; }
        // 所有运算在产物副本上完成；成功后才消耗材料，避免失败丢刀。
        out = out.copy();
        ItemStack tsuba = stacks().stream().filter(this::isTsuba).findFirst().orElse(ItemStack.EMPTY);
        boolean gold = tsuba.is(GeneratedMod.TSUBA_WOOD_GOLD.get())
                || tsuba.is(GeneratedMod.TSUBA_BAMBOO_GOLD.get())
                || tsuba.is(GeneratedMod.TSUBA_PURE_GOLD.get());
        ItemStack blank = find(GeneratedMod.QUENCHED_BLADE.get());
        int attack = (gold ? 2 : 0) + BladeData.ratioAttackBonus(blank);
        int durability = (gold ? 1 : 5) + BladeData.ratioDurabilityBonus(blank);
        ItemStack source = inputs.stream().filter(BladeData::isSlashBlade).findFirst().orElse(ItemStack.EMPTY);
        ItemStack definition = recipe.output(level);
        if (recipe.basic()) {
            BladeData.markCompatibleBlade(out, attack, durability, gold ? "gold" : "iron",
                    out.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(BladeData.SLASHBLADE_BAMBOO)));
        } else {
            if (!source.isEmpty()) BladeData.inheritProgress(source, out, definition);
            BladeData.applyUpgradeBonus(out, attack, durability, gold ? "gold" : "iron");
        }
        BladeData.rememberDefinition(out, definition);
        List<ItemStack> remains = recipe.remainders(inputs, level);
        for (int i = 0; i < SLOT_COUNT; i++) inventory.removeItemNoUpdate(i);
        giveBack(player, out);
        for (ItemStack remainder : remains) giveBack(player, remainder);
        tell(player, recipe.displayName() + "锻造完成！原刀的杀敌数、耀魂数、精炼数和攻击养成已保留。");
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.75f);
        level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.8f, 1.1f);
        sync();
    }
    private ItemStack find(net.minecraft.world.item.Item item) {
        return stacks().stream().filter(s -> s.is(item)).findFirst().orElse(ItemStack.EMPTY);
    }
    private boolean isTsuba(ItemStack stack) {
        return stack.is(GeneratedMod.TSUBA_WOOD_IRON.get()) || stack.is(GeneratedMod.TSUBA_WOOD_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_BAMBOO_IRON.get()) || stack.is(GeneratedMod.TSUBA_BAMBOO_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_PURE_IRON.get()) || stack.is(GeneratedMod.TSUBA_PURE_GOLD.get());
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
