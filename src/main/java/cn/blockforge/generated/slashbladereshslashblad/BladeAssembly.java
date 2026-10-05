package cn.blockforge.generated.slashbladereshslashblad;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 木偶（木刀）与竹刀的"手持刀条右键组装"流程（服务端为主）。
 *
 * <p>主手或副手持木刀条／竹刀条右键：背包内备齐对应刀镡、刀柄、刀鞘各一件时，
 * 立即消耗材料并广播组装动画（刀镡上刀 → 刀柄安装 → 插入刀鞘），
 * 动画播完把成品刀放进背包；缺任何一件则提示"背包内没有完整合成材料"。</p>
 *
 * <p>产物数值与刀剑制作台的木刀／竹刀内置配方完全一致：
 * 铁刀镡 攻+0 耐+5，铜刀镡 攻+1 耐+1，金刀镡 攻+2 耐+1（木/竹刀条不经五枚锭材锻打，不叠配比修正），
 * 动画时长、各阶段刻数见下面的常量，客户端按同一时间轴渲染部件飞行。</p>
 */
public final class BladeAssembly {
    /** 组装动画总时长（游戏刻），客户端与服务端共用同一时间轴。 */
    public static final int DURATION_TICKS = 80;
    /** 刀镡滑到刀条根部并"咔哒"落座的时刻。 */
    public static final int TSUBA_SNAP_TICK = 28;
    /** 刀柄从下方推上刀茎并落座的时刻。 */
    public static final int HANDLE_SNAP_TICK = 50;
    /** 整刀插入刀鞘、鞘口合拢的时刻。 */
    public static final int SHEATHE_SNAP_TICK = 66;

    /** 组装类型：木偶、竹光、名刀·寒霜。 */
    public static final int KIND_WOOD = 0, KIND_BAMBOO = 1, KIND_FROST = 2;
    /** 刀镡金属类型（与网络包 StartPayload.metal 一致）。 */
    public static final int METAL_IRON = 0, METAL_GOLD = 1, METAL_COPPER = 2;

    /** 玩家 UUID → 进行中的组装会话（仅服务端）。 */
    private record Session(int entityId, long startTick, int kind, int metal, Item tsuba,
                           ResourceKey<Level> levelKey) {
    }

    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private BladeAssembly() {
    }

    /** 该玩家是否还有一段组装动画在播（动画期间不允许再次发起）。 */
    public static boolean isBusy(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** 该刀条对应的刀镡：木条配木镡（含铜木）、竹条配竹镡（含铜竹）；金＞铜＞铁，攻击修正高者优先。 */
    public static Item findTsuba(Player player, boolean bamboo) {
        Item gold = bamboo ? GeneratedMod.TSUBA_BAMBOO_GOLD.get() : GeneratedMod.TSUBA_WOOD_GOLD.get();
        Item copper = bamboo ? GeneratedMod.TSUBA_BAMBOO_COPPER.get() : GeneratedMod.TSUBA_WOOD_COPPER.get();
        Item iron = bamboo ? GeneratedMod.TSUBA_BAMBOO_IRON.get() : GeneratedMod.TSUBA_WOOD_IRON.get();
        if (countInInventory(player, gold) > 0) return gold;
        if (countInInventory(player, copper) > 0) return copper;
        if (countInInventory(player, iron) > 0) return iron;
        return null;
    }

    /** 背包（含双手）是否备齐木偶/竹光的刀镡、刀柄、刀鞘各一件。 */
    public static boolean hasMaterials(Player player, boolean bamboo) {
        return findTsuba(player, bamboo) != null
                && countInInventory(player, GeneratedMod.BLADE_HANDLE.get()) > 0
                && countInInventory(player, GeneratedMod.BLADE_SHEATH.get()) > 0;
    }

    /** 寒霜需要任意纯金属刀镡、刀柄、刀鞘、雪块各一件。 */
    public static boolean hasFrostMaterials(Player player) {
        return findFrostTsuba(player) != null
                && countInInventory(player, GeneratedMod.BLADE_HANDLE.get()) > 0
                && countInInventory(player, GeneratedMod.BLADE_SHEATH.get()) > 0
                && countInInventory(player, net.minecraft.world.item.Items.SNOW_BLOCK) > 0;
    }

    /** 寒霜使用金＞铜＞铁的纯金属刀镡选择顺序。 */
    public static Item findFrostTsuba(Player player) {
        for (Item item : new Item[]{GeneratedMod.TSUBA_PURE_GOLD.get(), GeneratedMod.TSUBA_PURE_COPPER.get(),
                GeneratedMod.TSUBA_PURE_IRON.get()}) {
            if (countInInventory(player, item) > 0) return item;
        }
        return null;
    }

    /** 服务端入口：复核材料 → 消耗 → 登记会话并广播动画。返回 false 表示未开始。 */
    public static boolean tryStart(ServerPlayer player, ItemStack heldBlank, boolean bamboo) {
        if (isBusy(player)) {
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_busy");
            return false;
        }
        if (!hasMaterials(player, bamboo)) {
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_missing_materials");
            return false;
        }
        Item tsuba = findTsuba(player, bamboo);
        int metal = metalOf(tsuba, bamboo);
        consumeOne(player, tsuba);
        consumeOne(player, GeneratedMod.BLADE_HANDLE.get());
        consumeOne(player, GeneratedMod.BLADE_SHEATH.get());
        heldBlank.shrink(1);
        long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(),
                new Session(player.getId(), now, bamboo ? KIND_BAMBOO : KIND_WOOD, metal, tsuba,
                        player.level().dimension()));
        BladeAssemblyNetwork.broadcastStart(player, bamboo ? KIND_BAMBOO : KIND_WOOD, metal, now);
        Level level = player.level();
        level.playSound(null, player, bamboo ? InspectSounds.SPIN_BAMBOO.get() : InspectSounds.SPIN_WOOD.get(),
                SoundSource.PLAYERS, 0.9f, bamboo ? 1.25f : 0.85f);
        return true;
    }

    /** 服务端入口：成品刀条右键组装名刀·寒霜。 */
    public static boolean tryStartFrost(ServerPlayer player, ItemStack heldBlade) {
        if (isBusy(player)) {
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_busy");
            return false;
        }
        if (!hasFrostMaterials(player)) {
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_missing_materials");
            return false;
        }
        Item tsuba = findFrostTsuba(player);
        int metal = metalOfPure(tsuba);
        consumeOne(player, tsuba);
        consumeOne(player, GeneratedMod.BLADE_HANDLE.get());
        consumeOne(player, GeneratedMod.BLADE_SHEATH.get());
        consumeOne(player, net.minecraft.world.item.Items.SNOW_BLOCK);
        heldBlade.shrink(1);
        long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new Session(player.getId(), now, KIND_FROST, metal, tsuba,
                player.level().dimension()));
        BladeAssemblyNetwork.broadcastStart(player, KIND_FROST, metal, now);
        player.level().playSound(null, player, InspectSounds.SPIN_WOOD.get(), SoundSource.PLAYERS, 0.9f, 1.55f);
        return true;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        if (player.isRemoved() || !player.level().dimension().equals(session.levelKey())) {
            ACTIVE.remove(player.getUUID());
            complete(player, session);
            return;
        }
        long elapsed = player.level().getGameTime() - session.startTick();
        if (player.isDeadOrDying() || elapsed < 0 || elapsed > DURATION_TICKS + 200L) {
            ACTIVE.remove(player.getUUID());
            complete(player, session);
            return;
        }
        // 三段"咔哒"落座音与客户端部件到位的刻数一一对齐。
        if (elapsed == TSUBA_SNAP_TICK) knock(player, session, 1.0f);
        else if (elapsed == HANDLE_SNAP_TICK) knock(player, session, 1.15f);
        else if (elapsed == SHEATHE_SNAP_TICK) knock(player, session, 1.45f);
        else if (elapsed >= DURATION_TICKS) {
            ACTIVE.remove(player.getUUID());
            complete(player, session);
        }
    }

    /** 组装完成：寒霜使用 named_blades 定义，木偶/竹光沿用原有基础刀。 */
    private static void complete(ServerPlayer player, Session session) {
        if (session.kind() == KIND_FROST) {
            ItemStack out = BladeData.frostBlade(player.level().registryAccess());
            if (out.isEmpty()) {
                giveBack(player, new ItemStack(session.tsuba()));
                giveBack(player, new ItemStack(GeneratedMod.BLADE_HANDLE.get()));
                giveBack(player, new ItemStack(GeneratedMod.BLADE_SHEATH.get()));
                giveBack(player, new ItemStack(net.minecraft.world.item.Items.SNOW_BLOCK));
                giveBack(player, new ItemStack(GeneratedMod.QUENCHED_BLADE.get()));
                tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_failed");
                return;
            }
            BladeData.markBlade(out, 0, 0, guardName(session.metal()));
            // 自带附魔（力量 V · 灵魂疾行 II · 荆棘 III）现在由 named_blades/frost.json
            // 的 enchantments 提供，与其他妖刀同一机制，在 frostBlade() 内已挂好。
            player.getInventory().placeItemBackInInventory(out);
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_done_frost");
            player.level().playSound(null, player, SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 0.8f, 1.35f);
            return;
        }
        boolean bamboo = session.kind() == KIND_BAMBOO;
        ItemStack definition = BladeData.sword(bamboo ? BladeData.SLASHBLADE_BAMBOO : BladeData.SLASHBLADE_WOOD);
        ItemStack out = BladeData.sword(bamboo ? BladeData.SLASHBLADE_BAMBOO : BladeData.SLASHBLADE_WOOD);
        if (out.isEmpty()) {
            // 前置缺失的兜底：把刚消耗的材料原样退回，绝不静默吞物品。
            giveBack(player, new ItemStack(session.tsuba()));
            giveBack(player, new ItemStack(GeneratedMod.BLADE_HANDLE.get()));
            giveBack(player, new ItemStack(GeneratedMod.BLADE_SHEATH.get()));
            giveBack(player, new ItemStack(bamboo ? GeneratedMod.BAMBOO_BLADE_BLANK.get()
                    : GeneratedMod.WOODEN_BLADE_BLANK.get()));
            tell(player, "msg." + GeneratedMod.MOD_ID + ".assembly_failed");
            return;
        }
        int attack = switch (session.metal()) {
            case METAL_GOLD -> 2;
            case METAL_COPPER -> 1;
            default -> 0;
        };
        int durability = switch (session.metal()) {
            case METAL_GOLD, METAL_COPPER -> 1;
            default -> 5;
        };
        BladeData.markCompatibleBlade(out, attack, durability, guardName(session.metal()), bamboo);
        BladeData.rememberDefinition(out, definition);
        player.getInventory().placeItemBackInInventory(out);
        tell(player, "msg." + GeneratedMod.MOD_ID + (bamboo ? ".assembly_done_bamboo" : ".assembly_done_wood"));
        player.level().playSound(null, player, SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    /** 按刀镡物品判定金属类型（金/铜/铁）。 */
    private static int metalOf(Item tsuba, boolean bamboo) {
        if (tsuba == (bamboo ? GeneratedMod.TSUBA_BAMBOO_GOLD.get() : GeneratedMod.TSUBA_WOOD_GOLD.get())) return METAL_GOLD;
        if (tsuba == (bamboo ? GeneratedMod.TSUBA_BAMBOO_COPPER.get() : GeneratedMod.TSUBA_WOOD_COPPER.get())) return METAL_COPPER;
        return METAL_IRON;
    }

    private static int metalOfPure(Item tsuba) {
        if (tsuba == GeneratedMod.TSUBA_PURE_GOLD.get()) return METAL_GOLD;
        if (tsuba == GeneratedMod.TSUBA_PURE_COPPER.get()) return METAL_COPPER;
        return METAL_IRON;
    }

    /** 写进刀数据里的刀镡材质名。 */
    private static String guardName(int metal) {
        return switch (metal) {
            case METAL_GOLD -> "gold";
            case METAL_COPPER -> "copper";
            default -> "iron";
        };
    }

    private static void knock(ServerPlayer player, Session session, float pitch) {
        player.level().playSound(null, player,
                session.kind() == KIND_BAMBOO ? InspectSounds.KNOCK_BAMBOO.get() : InspectSounds.KNOCK_WOOD.get(),
                SoundSource.PLAYERS, 1.0f, (session.kind() == KIND_BAMBOO ? 1.25f : 0.85f) * pitch);
    }

    private static int countInInventory(Player player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static boolean consumeOne(Player player, Item item) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
