package cn.blockforge.generated.slashbladereshslashblad.item;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** 过火后刚出炉的灼热失败刀条，冷却后才会变成失败的刀条。 */
public class HotFailedBladeItem extends Item implements HotMetal.Hot {
    public static final int COOL_TICKS = 300;

    public HotFailedBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HotMetal.ignite(target);
        return super.hurtEnemy(stack, target, attacker);
    }

    /** 只显示冷却倒计时，不加其他说明文字。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        long since = BladeData.hotSince(stack);
        long age = context == null || context.level() == null ? 0 : context.level().getGameTime() - since;
        long left = Math.max(0, (COOL_TICKS - age + 19) / 20);
        tooltip.add(Component.literal("冷却 " + left + "s"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(Blocks.WATER_CAULDRON)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            context.getPlayer().setItemInHand(context.getHand(), new ItemStack(GeneratedMod.FAILED_BLADE.get()));
            level.playSound(null, context.getClickedPos(), SoundEvents.GENERIC_SPLASH,
                    SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static ItemStack coolDown(ItemStack hot) {
        return new ItemStack(GeneratedMod.FAILED_BLADE.get(), hot.getCount());
    }
}
