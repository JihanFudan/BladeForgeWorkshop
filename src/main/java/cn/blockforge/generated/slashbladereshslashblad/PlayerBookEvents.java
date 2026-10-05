package cn.blockforge.generated.slashbladereshslashblad;

import cn.blockforge.generated.slashbladereshslashblad.item.GuideBookItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 《锻刀工坊游玩手册》的发放与合成。
 *
 * <p>发放只发生在玩家<b>初次进入本存档</b>时：给没给记在玩家的存档数据里
 * （{@code PersistentData}，会随 player.dat 存盘），所以死亡重生、退出重进
 * 都不会再补发。旧版在重生事件里也发一次，玩家死几本手册就叠几本。</p>
 *
 * <p>想要第二本，改为手动合成：工作台或背包合成格里放一本书 + 一把锻造锤
 * （无序），产出一本手册；锻造锤在这里只当工具用，合成完原样退回
 * （见 {@link #onGuideBookCrafted}）。</p>
 */
public final class PlayerBookEvents {
    /** 玩家存档数据上的标记：这位玩家在本存档已经处理过手册发放。 */
    private static final String GIVEN = "BladeForgeGuideBookGiven";

    private PlayerBookEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (player.getPersistentData().getBoolean(GIVEN)) {
            return;
        }
        // 标记先落下：这一次无论最终给不给，以后都不会再自动发。
        player.getPersistentData().putBoolean(GIVEN, true);
        if (player.getInventory().contains(stack -> stack.is(GeneratedMod.GUIDE_BOOK.get()))) {
            return; // 背包里已经有一本，不再多发
        }
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) {
            return; // 玩过一段时间的老玩家不算“初次进入世界”，不该因为更新多拿一本
        }
        give(player);
    }

    /**
     * 手册的合成回报：产出《游玩手册》时把当工具用的锻造锤退回给玩家。
     *
     * <p>产出该手册的配方只有「书 + 锻造锤」这一条，而且原版 {@link PlayerEvent.ItemCraftedEvent}
     * 触发时合成格往往已经被清空，扫描格子并不可靠，所以这里直接按配方应当退还的数量归还。
     * 锻造锤没有耐久与附加数据，重新给一把与原物完全等价。</p>
     */
    @SubscribeEvent
    public static void onGuideBookCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (!event.getCrafting().is(GeneratedMod.GUIDE_BOOK.get())) {
            return;
        }
        ItemStack hammer = new ItemStack(GeneratedMod.FORGING_HAMMER.get());
        if (!player.getInventory().add(hammer)) {
            player.drop(new ItemStack(GeneratedMod.FORGING_HAMMER.get()), false);
        }
    }

    private static void give(ServerPlayer player) {
        ItemStack book = GuideBookItem.written(GeneratedMod.GUIDE_BOOK.get());
        if (!player.getInventory().add(book)) {
            player.drop(GuideBookItem.written(GeneratedMod.GUIDE_BOOK.get()), false);
        }
        player.displayClientMessage(Component.literal("已发放《游玩手册》，拿在手里右键即可阅读。"), false);
    }
}
