package cn.blockforge.generated.slashbladereshslashblad.blockentity;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 烧铁炉的方块实体：一次最多放 5 枚（个）可烧材料，约 8 秒烧红。
 * 可烧：铁锭→灼热铁锭、钢锭→灼热钢锭、融合钢→灼热融合钢、粗制刀条→灼热粗制刀条、
 * 覆土刀条→灼热覆土刀条；未完成的刀条配黏土可直接覆土入炉。
 * 烧好后炉膛滚烫，背包（含快捷栏与副手）里有钳子就能右键夹取。炉口悬浮显示物品并逐渐变亮（客户端渲染器）。
 */
public class HeatingFurnaceBlockEntity extends BlockEntity {
    /** 一批烧制耗时（8 秒）。 */
    public static final int HEAT_TICKS = 160;
    /** 一次最多塞进炉膛的数量。 */
    public static final int BATCH_MAX = 5;

    private final SimpleContainer inventory = new SimpleContainer(1);
    private long heatStart;
    private boolean done;

    public HeatingFurnaceBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static HeatingFurnaceBlockEntity create(BlockPos pos, BlockState state) {
        return new HeatingFurnaceBlockEntity(GeneratedMod.HEATING_FURNACE_ENTITY.get(), pos, state);
    }

    public SimpleContainer items() {
        return inventory;
    }

    public boolean done() {
        return done;
    }

    /** 渲染器用的 0..1 烧制进度（客户端本地按时间推算，不需要每刻同步）。 */
    public float heat(Level level, float partialTick) {
        if (inventory.getItem(0).isEmpty()) {
            return 0f;
        }
        if (done) {
            return 1f;
        }
        float t = (float) (level.getGameTime() + partialTick - heatStart) / HEAT_TICKS;
        return Math.clamp(t, 0f, 1f);
    }

    /** 该物品烧制后的产物；不可烧返回 null。 */
    public static Item heatedResult(ItemStack stack) {
        if (stack.is(Items.IRON_INGOT)) {
            return GeneratedMod.HEATED_IRON.get();
        }
        if (stack.is(GeneratedMod.STEEL_INGOT.get())) {
            return GeneratedMod.HEATED_STEEL.get();
        }
        if (stack.is(GeneratedMod.LOW_CARBON_STEEL.get())) {
            return GeneratedMod.HEATED_LOW_CARBON.get();
        }
        if (stack.is(GeneratedMod.HIGH_CARBON_STEEL.get())) {
            return GeneratedMod.HEATED_HIGH_CARBON.get();
        }
        if (stack.is(GeneratedMod.FUSED_STEEL.get())) {
            return GeneratedMod.HEATED_FUSED_STEEL.get();
        }
        if (stack.is(GeneratedMod.CRUDE_BLADE.get())) {
            return GeneratedMod.HEATED_CRUDE_BLADE.get();
        }
        if (stack.is(GeneratedMod.CLAY_BLADE.get())) {
            return GeneratedMod.HEATED_CLAY_BLADE.get();
        }
        return null;
    }

    /** 烧好后仍画冷态轮廓并染红，使完成一刻也不会跳换贴图。 */
    public static ItemStack coldDisplay(ItemStack stack) {
        Item cold = stack.getItem();
        if (stack.is(GeneratedMod.HEATED_IRON.get())) cold = Items.IRON_INGOT;
        else if (stack.is(GeneratedMod.HEATED_STEEL.get())) cold = GeneratedMod.STEEL_INGOT.get();
        else if (stack.is(GeneratedMod.HEATED_LOW_CARBON.get())) cold = GeneratedMod.LOW_CARBON_STEEL.get();
        else if (stack.is(GeneratedMod.HEATED_HIGH_CARBON.get())) cold = GeneratedMod.HIGH_CARBON_STEEL.get();
        else if (stack.is(GeneratedMod.HEATED_FUSED_STEEL.get())) cold = GeneratedMod.FUSED_STEEL.get();
        else if (stack.is(GeneratedMod.HEATED_CRUDE_BLADE.get())) cold = GeneratedMod.CRUDE_BLADE.get();
        else if (stack.is(GeneratedMod.HEATED_CLAY_BLADE.get())) cold = GeneratedMod.CLAY_BLADE.get();
        return new ItemStack(cold, stack.getCount());
    }

    private static boolean isHeated(ItemStack stack) {
        return stack.is(GeneratedMod.HEATED_IRON.get()) || stack.is(GeneratedMod.HEATED_STEEL.get())
                || stack.is(GeneratedMod.HEATED_LOW_CARBON.get()) || stack.is(GeneratedMod.HEATED_HIGH_CARBON.get())
                || stack.is(GeneratedMod.HEATED_FUSED_STEEL.get()) || stack.is(GeneratedMod.HEATED_CRUDE_BLADE.get())
                || stack.is(GeneratedMod.HEATED_CLAY_BLADE.get()) || stack.is(GeneratedMod.HOT_BLADE.get());
    }

    public void dropStoredItems(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, inventory);
    }

    public ItemInteractionResult interactFurnace(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        ItemStack stored = inventory.getItem(0);
        // 一、烧好了：背包里有钳子就能夹取（不再要求钳子必须拿在手上）
        if (!stored.isEmpty() && done) {
            if (held.is(GeneratedMod.TONGS.get())
                    || cn.blockforge.generated.slashbladereshslashblad.ForgeEvents.hasTongsInInventory(player)) {
                giveBack(player, inventory.removeItemNoUpdate(0));
                done = false;
                heatStart = 0;
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.3f);
                tell(player, "用钳子夹出来了！趁热放上锻造铁砧。");
                sync();
            } else {
                tell(player, "炉膛烫得吓人，徒手够不着——把钳子放进背包再右键取件。");
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 二、覆土：未完成的刀条 + 黏土（两只手任意组合），覆土后直接入炉烧红
        if (stored.isEmpty()) {
            ItemStack offhand = player.getOffhandItem() == held ? player.getMainHandItem() : player.getOffhandItem();
            if ((held.is(GeneratedMod.UNFINISHED_BLADE.get()) && offhand.is(Items.CLAY_BALL))
                    || (offhand.is(GeneratedMod.UNFINISHED_BLADE.get()) && held.is(Items.CLAY_BALL))) {
                ItemStack blade = held.is(GeneratedMod.UNFINISHED_BLADE.get()) ? held : offhand;
                ItemStack clay = held.is(Items.CLAY_BALL) ? held : offhand;
                ItemStack clayed = new ItemStack(GeneratedMod.CLAY_BLADE.get());
                BladeData.copyRatio(blade, clayed);
                blade.shrink(1);
                clay.shrink(1);
                inventory.setItem(0, clayed);
                heatStart = level.getGameTime();
                done = false;
                level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                tell(player, "黏土已覆面，直接在炉内烧红，约 8 秒后用钳子夹取。");
                sync();
                return ItemInteractionResult.SUCCESS;
            }
            // 三、普通入炉
            Item result = heatedResult(held);
            if (result != null) {
                int n = Math.min(held.getCount(), BATCH_MAX);
                ItemStack in = held.copyWithCount(n);
                held.shrink(n);
                inventory.setItem(0, in);
                heatStart = level.getGameTime();
                done = false;
                level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.9f, 1.0f);
                tell(player, "已放进烧铁炉（一次最多 " + BATCH_MAX + " 个）。盯着炉口，它会一点点烧红。");
                sync();
                return ItemInteractionResult.SUCCESS;
            }
            if (held.is(GeneratedMod.UNFINISHED_BLADE.get())) {
                tell(player, "未完成的刀条要先覆土：另一只手拿一团黏土，再右键炉子即可覆土入炉。");
                return ItemInteractionResult.SUCCESS;
            }
            if (isHeated(held)) {
                tell(player, "它已经烧红了，直接拿去锻造铁砧。");
                return ItemInteractionResult.SUCCESS;
            }
            if (held.isEmpty()) {
                tell(player, "烧铁炉可烧：铁锭、钢锭、低碳钢、高碳钢、融合钢、粗制刀条、覆土刀条；烧好后用钳子夹取。");
                return ItemInteractionResult.SUCCESS;
            }
            tell(player, "炉子不收这个。可烧：铁锭、钢锭、低碳钢、高碳钢、融合钢、粗制刀条、覆土刀条。");
            return ItemInteractionResult.SUCCESS;
        }
        // 四、正在烧
        int left = (int) Math.max(0, (HEAT_TICKS - (level.getGameTime() - heatStart) + 10) / 20);
        if (held.is(GeneratedMod.TONGS.get()) || held.isEmpty()) {
            tell(player, "还在烧，约 " + left + " 秒后烧红。");
        } else {
            tell(player, "炉膛里正烧着，先别塞新的。");
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 服务端刻：推进烧制，到点换产物并同步。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatingFurnaceBlockEntity be) {
        ItemStack stored = be.inventory.getItem(0);
        if (stored.isEmpty() || be.done) {
            return;
        }
        Item result = heatedResult(stored);
        if (result == null) {
            be.done = true;
            be.sync();
            return;
        }
        if (level.getGameTime() - be.heatStart >= HEAT_TICKS) {
            ItemStack out = new ItemStack(result, stored.getCount());
            BladeData.copyRatio(stored, out);
            be.inventory.setItem(0, out);
            be.done = true;
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25f, 1.6f);
            be.sync();
        }
    }

    private static void tell(Player player, String text) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heatStart = tag.getLong("HeatStart");
        done = tag.getBoolean("Done");
        inventory.setItem(0, ItemStack.parseOptional(registries, tag.getCompound("Slot0")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("HeatStart", heatStart);
        tag.putBoolean("Done", done);
        // 1.21.1 中空物品栈调用 save() 会抛 IllegalStateException（拆方块同步数据时必崩），必须用 saveOptional。
        tag.put("Slot0", inventory.getItem(0).saveOptional(registries));
    }
}
