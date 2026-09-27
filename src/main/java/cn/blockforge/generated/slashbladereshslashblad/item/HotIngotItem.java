package cn.blockforge.generated.slashbladereshslashblad.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 烧红的锭子（灼热铁锭 / 灼热钢锭）：刚从加热炉里夹出来的那一块。
 * 自带 1 点伤害、打中点燃目标；背包里没钳子就每秒烫自己 1 滴血。
 */
public class HotIngotItem extends Item implements HotMetal.Hot {
    public HotIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HotMetal.ignite(target);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("烧红：攻击 +1，命中点燃目标"));
        tooltip.add(Component.literal("背包里没放钳子时，每秒烫掉自己 1 滴血"));
    }
}
