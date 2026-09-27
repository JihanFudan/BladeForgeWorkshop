package cn.blockforge.generated.slashbladereshslashblad;

import cn.blockforge.generated.slashbladereshslashblad.item.HotBladeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 全局流程事件：烫手刀条 30 秒自然冷却；双手（工具+板材）加工木/竹刀条。 */
public final class ForgeEvents {
    private ForgeEvents() {
    }

    private static final net.minecraft.resources.ResourceLocation MINIMUM_ATTACK =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "minimum_blade_attack");

    /** 虚弱可能把攻击降到零，原版会直接跳过伤害流程，因此攻击前先补基础下限。 */
    private static void refreshMinimumAttack(Player player) {
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attribute == null) return;
        attribute.removeModifier(MINIMUM_ATTACK);
        ItemStack stack = player.getMainHandItem();
        if (!BladeData.isSlashBlade(stack)
                || !BladeData.data(stack).getCompound("SlashBladeResharpedForge").getBoolean("Blade")) return;
        double value = attribute.getValue();
        if (value < 1.0) attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                MINIMUM_ATTACK, 1.0 - value, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent
    public static void beforeBladeAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        refreshMinimumAttack(event.getEntity());
    }

    @SubscribeEvent
    public static void refreshBladeFloor(PlayerTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide()) refreshMinimumAttack(event.getEntity());
    }

    /** 弱化与减伤计算结束后，仅为本附属制作的刀补足最低一点；不干预取消的攻击。 */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void minimumBladeDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) return;
        ItemStack stack = player.getMainHandItem();
        if (BladeData.isSlashBlade(stack) && BladeData.data(stack).getCompound("SlashBladeResharpedForge").getBoolean("Blade")
                && event.getNewDamage() < 1f) {
            event.setNewDamage(1f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        // 烧红的工件：背包里没有钳子就一直烫手，每秒 1 滴血（半颗心）。
        // 每秒结算一次而不是每 tick —— 每 tick 扣会被无敌帧吃掉，反而一点都不掉。
        if (player.tickCount % 20 == 0) {
            burnHotHands(player);
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        long now = player.level().getGameTime();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            BladeData.migrateLegacy(stack);
            if (stack.is(GeneratedMod.HOT_BLADE.get()) && now - BladeData.hotSince(stack) >= HotBladeItem.COOL_TICKS) {
                inventory.setItem(i, HotBladeItem.coolDown(stack));
                player.displayClientMessage(Component.literal("刀条自然冷却，现在是可用于制刀的成品刀条。"), true);
            }
        }
    }

    /** 手上拿着烧红的金属、背包（含快捷栏）里又没钳子，就每秒烫掉 1 滴血。 */
    private static void burnHotHands(ServerPlayer player) {
        boolean hot = cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.isHot(player.getMainHandItem())
                || cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.isHot(player.getOffhandItem());
        if (!hot) {
            return;
        }
        if (hasTongsInInventory(player)) {
            // 背包里有钳子，随时能夹着工件，这是正确拿法，不烫。
            return;
        }
        cn.blockforge.generated.slashbladereshslashblad.item.HotMetal.burnSelf(player);
    }

    /** 钳子判定：背包（含快捷栏 36 格）和副手都算；只看手上拿没拿会漏掉“一手钳子一手工件”的拿法。 */
    public static boolean hasTongsInInventory(Player player) {
        if (player.getOffhandItem().is(GeneratedMod.TONGS.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(GeneratedMod.TONGS.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 木质刀条新工序（用户要求：一只手拿木板、一只手拿切割刀）：
     * 切割刀 + 木板/竹板 右键 → 削出「刀条雏形」（削木头音效）；
     * 雕刻凿 + 刀条雏形 右键 → 雕成「木刀条/竹刀条」（继续削/凿音效）。
     * 两只手谁拿工具都算。
     */
    @SubscribeEvent
    public static void onTwoHandWork(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        if (level.isClientSide || event.getEntity().isCrouching()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        ItemStack other = player.getItemInHand(event.getHand() == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        // ① 切割刀 + 板材 → 刀条雏形
        ItemStack knife = null;
        ItemStack board = null;
        if (held.is(GeneratedMod.CUTTING_KNIFE.get()) && boardOf(other) != null) {
            knife = held;
            board = other;
        } else if (other.is(GeneratedMod.CUTTING_KNIFE.get()) && boardOf(held) != null) {
            knife = other;
            board = held;
        }
        if (knife != null && board != null) {
            boolean bamboo = board.is(Items.BAMBOO_PLANKS);
            board.shrink(1);
            give(player, new ItemStack(bamboo ? GeneratedMod.BAMBOO_ROUGH.get() : GeneratedMod.WOODEN_ROUGH.get()));
            playWorkSound(level, player, bamboo ? SoundEvents.BAMBOO_BREAK : SoundEvents.AXE_STRIP);
            tell(player, "削出" + (bamboo ? "了竹质刀条雏形！" : "了木质刀条雏形！")
                    + "换雕刻凿（另一只手拿也行）右键一下，雕成刀条。");
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        // ② 雕刻凿 + 刀条雏形 → 刀条
        ItemStack chisel = null;
        ItemStack rough = null;
        if (held.is(GeneratedMod.CARVING_CHISEL.get()) && roughOf(other) != null) {
            chisel = held;
            rough = other;
        } else if (other.is(GeneratedMod.CARVING_CHISEL.get()) && roughOf(held) != null) {
            chisel = other;
            rough = held;
        }
        if (chisel != null && rough != null) {
            boolean bamboo = rough.is(GeneratedMod.BAMBOO_ROUGH.get());
            rough.shrink(1);
            give(player, new ItemStack(bamboo ? GeneratedMod.BAMBOO_BLADE_BLANK.get() : GeneratedMod.WOODEN_BLADE_BLANK.get()));
            playWorkSound(level, player, SoundEvents.AXE_STRIP);
            tell(player, (bamboo ? "竹刀条" : "木刀条") + "雕好了！手持它右键，配齐背包里的刀镡、刀柄、刀鞘即可现场组装。");
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    /** 可用于切割的板材：返回对应产物（竹板走竹路线），不可用返回 null。 */
    private static Boolean boardOf(ItemStack stack) {
        if (stack.is(Items.BAMBOO_PLANKS) || stack.is(net.minecraft.tags.ItemTags.PLANKS)) {
            return Boolean.TRUE;
        }
        return null;
    }

    private static Boolean roughOf(ItemStack stack) {
        if (stack.is(GeneratedMod.WOODEN_ROUGH.get()) || stack.is(GeneratedMod.BAMBOO_ROUGH.get())) {
            return Boolean.TRUE;
        }
        return null;
    }

    private static void playWorkSound(Level level, Player player, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.1, player.getZ(), 6, 0.25, 0.08, 0.25, 0.01);
        }
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void tell(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
