package cn.blockforge.generated.slashbladereshslashblad.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 锻造工具（切割刀、雕刻凿、锻造锤、钳子）。 */
public class ForgingToolItem extends Item {
    public ForgingToolItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    /** 钳子多说一句它真正的用处：放在背包里就能夹住烧红工件，手不会被烫。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.is(cn.blockforge.generated.slashbladereshslashblad.GeneratedMod.TONGS.get())) {
            tooltip.add(Component.literal("拿在手上或放在背包里都算；拿着烧红工件时会显示夹着的姿势"));
            tooltip.add(Component.literal("有钳子就不会被烫伤"));
        }
    }
}
