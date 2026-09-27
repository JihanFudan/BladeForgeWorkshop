package cn.blockforge.generated.slashbladereshslashblad.item;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 锻造链上的工件：悬浮提示显示融合用的金属种类与预期修正。 */
public class RatioItem extends Item {
    public RatioItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (BladeData.hasRatio(stack)) {
            if (BladeData.isMixedRatio(stack)) {
                tooltip.add(Component.literal("金属配比：" + BladeData.ratioMixSummary(stack) + "（融合钢）"));
            } else {
                tooltip.add(Component.literal("金属种类：" + BladeData.ratioGradeName(stack)));
            }
            tooltip.add(Component.literal("预期修正：攻击 " + signed(BladeData.ratioAttackBonus(stack))
                    + "，耐久 " + signed(BladeData.ratioDurabilityBonus(stack))));
        }
    }

    static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }
}
