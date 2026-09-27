package cn.blockforge.generated.slashbladereshslashblad.item;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 烫手的刀条：覆土刀条在铁砧上锤出的最终工件。
 * 右键有水炼药锅急速淬火；背包里没钳子硬拿会一直烫着，30 秒后自然冷却。
 * 两种途径都会把它变成「成品刀条」，才能用于制作拔刀剑。
 *
 * <p>它同时是"烧红的金属"：自带 1 点伤害、打中点燃目标，
 * 背包里没钳子时每秒烫掉自己 1 滴血（见 {@link HotMetal}）。</p>
 */
public class HotBladeItem extends RatioItem implements HotMetal.Hot {
    /** 30 秒 = 600 游戏刻。 */
    public static final int COOL_TICKS = 600;

    public HotBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HotMetal.ignite(target);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        long since = BladeData.hotSince(stack);
        long age = context == null ? 0 : context.level() == null ? 0 : context.level().getGameTime() - since;
        long left = Math.max(0, (COOL_TICKS - age) / 20);
        tooltip.add(Component.literal("烫手！右键有水炼药锅可急速降温"));
        tooltip.add(Component.literal("自然冷却还需约 " + left + " 秒"));
        tooltip.add(Component.literal("烧红：攻击 +1，命中点燃目标"));
        tooltip.add(Component.literal("背包里没放钳子时，每秒烫掉自己 1 滴血"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(Blocks.WATER_CAULDRON)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ItemStack cooled = coolDown(stack);
            if (context.getPlayer() != null) {
                context.getPlayer().setItemInHand(context.getHand(), cooled);
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 1.4f);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    context.getClickedPos().getX() + 0.5, context.getClickedPos().getY() + 1.1, context.getClickedPos().getZ() + 0.5,
                    24, 0.15, 0.1, 0.15, 0.02);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("刀条入水，嗤——淬火完成！"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 把烫手刀条换成成品刀条，配比原样保留。 */
    public static ItemStack coolDown(ItemStack hot) {
        ItemStack cooled = new ItemStack(GeneratedMod.QUENCHED_BLADE.get());
        BladeData.copyRatio(hot, cooled);
        return cooled;
    }
}
