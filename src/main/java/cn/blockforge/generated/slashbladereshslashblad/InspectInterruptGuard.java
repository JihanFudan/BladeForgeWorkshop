package cn.blockforge.generated.slashbladereshslashblad;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 检视打断守卫（仅服务端）。
 *
 * <p>重锋把连招状态写在刀自身的运行时数据里：一旦开始检视，即便玩家把刀切到手、
 * 打开背包或死亡，那把刀的 comboSeq 仍是检视动作，切回主手时动画与音效会"续播"
 * ——这正是用户反馈的"打断后声音还在放"。这里跟踪每把正在检视的刀，只要它不再是
 * 主手物品（或玩家死亡/旁观/换维度/下线/超时），立刻把它的连招强制清零到 none，
 * 使时间轴与音效当场停止，而不是等它自己播完。</p>
 */
public final class InspectInterruptGuard {

    /** 玩家 → 当前正在检视的那把刀（主手物品引用）。 */
    private static final Map<UUID, ItemStack> TRACKING = new ConcurrentHashMap<>();
    /** 玩家 → 跟踪过期的游戏刻（防止异常残留）。 */
    private static final Map<UUID, Long> EXPIRY = new ConcurrentHashMap<>();

    /** 检视总时长 202 帧 ≈ 6.7 秒（照抄 SlashBladeInspect），给足冗余再留 8 秒兜底。 */
    private static final long TRACK_TTL = 20 * 20L;

    private InspectInterruptGuard() {
    }

    /** 由 {@code handleSetCombo} 在成功发起检视后调用；会先清掉该玩家上一把仍在跟踪的刀。 */
    public static void track(ServerPlayer player, ItemStack blade) {
        ItemStack previous = TRACKING.put(player.getUUID(), blade);
        if (previous != null && previous != blade) {
            clearInspect(player, previous);
        }
        EXPIRY.put(player.getUUID(), player.level().getGameTime() + TRACK_TTL);
    }

    /** 客户端主动取消（再次按 G / 打开界面）时，服务端立即清零并解除跟踪。 */
    public static void cancel(ServerPlayer player) {
        ItemStack tracked = TRACKING.remove(player.getUUID());
        EXPIRY.remove(player.getUUID());
        clearInspect(player, player.getMainHandItem());
        if (tracked != null) {
            clearInspect(player, tracked);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ItemStack tracked = TRACKING.get(player.getUUID());
        if (tracked == null) {
            return;
        }
        boolean keep = player.level().getGameTime() <= EXPIRY.getOrDefault(player.getUUID(), 0L)
                && !player.isRemoved()
                && !player.isDeadOrDying()
                && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
                && player.getMainHandItem() == tracked
                && isInspecting(tracked);
        if (!keep) {
            clearInspect(player, tracked);
            TRACKING.remove(player.getUUID());
            EXPIRY.remove(player.getUUID());
        }
    }

    /** 换维度/重生会重建玩家与背包，旧引用失效，直接清理。 */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.LoadFromFile event) {
        UUID id = event.getEntity().getUUID();
        TRACKING.remove(id);
        EXPIRY.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        TRACKING.remove(id);
        EXPIRY.remove(id);
    }

    private static boolean isInspecting(ItemStack stack) {
        return BladeStateAccess.of(stack)
                .map(state -> InspectComboStates.isInspectId(state.getComboSeq()))
                .orElse(false);
    }

    private static void clearInspect(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        BladeStateAccess.of(stack).ifPresent(state -> {
            ResourceLocation combo = state.getComboSeq();
            if (InspectComboStates.isInspectId(combo)) {
                state.updateComboSeq(player, ComboStateRegistry.NONE.getId());
            }
        });
    }
}
