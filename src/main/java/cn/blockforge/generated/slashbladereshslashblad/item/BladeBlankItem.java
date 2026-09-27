package cn.blockforge.generated.slashbladereshslashblad.item;

import java.util.List;

import cn.blockforge.generated.slashbladereshslashblad.BladeAssembly;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 木刀条／竹刀条：手持右键即可现场组装成木偶（木刀）或竹刀。
 * 右键时检测背包（含双手）里是否有对应刀镡、刀柄、刀鞘各一件：
 * 齐全则消耗材料、播放"刀镡上刀→刀柄安装→插入刀鞘"的组装动画并出刀；
 * 缺件则在快捷栏上方提示"背包内没有完整合成材料"。
 */
public class BladeBlankItem extends Item {
    private final boolean bamboo;

    public BladeBlankItem(Properties properties, boolean bamboo) {
        super(properties);
        this.bamboo = bamboo;
    }

    public boolean bamboo() {
        return bamboo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (BladeAssembly.isBusy(player)) {
            player.displayClientMessage(
                    Component.translatable("msg." + GeneratedMod.MOD_ID + ".assembly_busy"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!BladeAssembly.hasMaterials(player, bamboo)) {
            // 客户端在这里直接提示并阻止发包；服务端兜底复核（防不同步或伪造）。
            player.displayClientMessage(
                    Component.translatable("msg." + GeneratedMod.MOD_ID + ".assembly_missing_materials"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !BladeAssembly.tryStart(serverPlayer, stack, bamboo)) {
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + GeneratedMod.MOD_ID + ".blade_blank_assemble")
                .withStyle(ChatFormatting.GRAY));
    }
}
