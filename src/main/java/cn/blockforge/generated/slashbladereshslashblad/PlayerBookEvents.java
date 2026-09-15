package cn.blockforge.generated.slashbladereshslashblad;

import cn.blockforge.generated.slashbladereshslashblad.item.GuideBookItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 玩家进入存档（含重生）时，若背包里没有《锻刀工坊游玩手册》就自动补发一本。 */
public final class PlayerBookEvents {
    private PlayerBookEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        giveGuideBook(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        giveGuideBook(event.getEntity());
    }

    private static void giveGuideBook(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer) {
            return;
        }
        if (player.getInventory().contains(stack -> stack.is(GeneratedMod.GUIDE_BOOK.get()))) {
            return;
        }
        ItemStack book = GuideBookItem.written(GeneratedMod.GUIDE_BOOK.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.displayClientMessage(Component.literal("锻刀工坊：已放入一本《游玩手册》，拿在手里右键即可阅读。"), false);
    }
}
