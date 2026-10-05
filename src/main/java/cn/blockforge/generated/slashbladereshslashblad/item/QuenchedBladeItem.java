package cn.blockforge.generated.slashbladereshslashblad.item;

import cn.blockforge.generated.slashbladereshslashblad.BladeAssembly;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

/** 成品刀条：右键空气可按寒霜专属材料组装名刀·寒霜。 */
public final class QuenchedBladeItem extends RatioItem {
    public QuenchedBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (BladeAssembly.isBusy(player)) {
            player.displayClientMessage(Component.translatable(
                    "msg." + GeneratedMod.MOD_ID + ".assembly_busy"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!BladeAssembly.hasFrostMaterials(player)) {
            player.displayClientMessage(Component.translatable(
                    "msg." + GeneratedMod.MOD_ID + ".assembly_missing_materials"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !BladeAssembly.tryStartFrost(serverPlayer, stack)) {
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + GeneratedMod.MOD_ID + ".quenched_frost"));
    }
}
