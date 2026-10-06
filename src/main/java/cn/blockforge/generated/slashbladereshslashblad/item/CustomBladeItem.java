package cn.blockforge.generated.slashbladereshslashblad.item;

import java.util.List;
import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 制作台组装出的拔刀剑；基础攻击 2、基础耐久 35，具体外观保存在物品数据中。 */
public final class CustomBladeItem extends ItemSlashBlade {
    public CustomBladeItem(Properties properties) {
        super(new ItemTierSlashBlade(35, 2.0F), 2, 0.0F, properties.stacksTo(1));
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "item.slashbladeresh_slashblad.custom_blade";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.slashbladeresh_slashblad.custom_blade_parts").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(BladeData.customPartSummary(stack)).withStyle(ChatFormatting.AQUA));
    }
}
