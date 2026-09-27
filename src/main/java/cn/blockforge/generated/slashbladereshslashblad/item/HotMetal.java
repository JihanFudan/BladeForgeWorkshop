package cn.blockforge.generated.slashbladereshslashblad.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;

/**
 * "烧红的金属"共有的几件事，刀条和锭子都走这里，免得同一套逻辑抄三遍：
 *
 * <ol>
 *   <li><b>自带 1 点伤害</b>：{@link #attackModifier()} 给物品挂 +1 攻击力，
 *       玩家空手本来是 1 点，拿着它就是 2 点。</li>
 *   <li><b>打中会点燃</b>：{@link #ignite(LivingEntity)}，被碰到的生物着火烧 4 秒。</li>
 *   <li><b>空手拿着会烫自己</b>：背包里没有钳子时每秒掉 1 滴血（半颗心）。
 *       这条在 {@link cn.blockforge.generated.slashbladereshslashblad.ForgeEvents}
 *       的玩家 tick 里结算——烫伤是"只要拿着就一直发生"，不属于物品自身的某个回调。</li>
 * </ol>
 *
 * <p>钳子的判定看背包（含快捷栏的 36 格）与副手：包里或副手有钳子就算有，
 * 主手拿着烧红工件也不影响。
 * 客户端要按同样的结果切换模型（见
 * {@link cn.blockforge.generated.slashbladereshslashblad.client.HeldBladeModels}），
 * 所以判定统一放这儿。</p>
 */
public final class HotMetal {
    private HotMetal() {
    }

    /** 攻击力修正的 id。 */
    private static final ResourceLocation ATTACK_ID =
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "hot_metal_attack");

    /** 被烧红工件打中后的着火秒数。 */
    public static final float IGNITE_SECONDS = 4.0f;

    /** 没钳子时每秒烫掉的伤害值：1.0 点 = 半颗心 = 用户说的"1 滴血"。 */
    public static final float SELF_DAMAGE_PER_SECOND = 1.0f;

    /** 给物品属性表挂上"+1 攻击力"，注册物品时 {@code props.attributes(...)} 用。 */
    public static ItemAttributeModifiers attackModifier() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** 打中目标就点燃。物品在自己的 {@code hurtEnemy} 里调一下即可。 */
    public static void ignite(LivingEntity target) {
        if (!target.fireImmune()) {
            target.igniteForSeconds(IGNITE_SECONDS);
        }
    }

    /** 烫自己一下（伤害类型用"着火"，死亡提示会是"被火烧死"）。创造/旁观不扣。 */
    public static void burnSelf(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.hurt(player.damageSources().inFire(), SELF_DAMAGE_PER_SECOND);
    }

    /** 这件物品是不是"烧红的金属"（客户端切模型、服务端算烫伤都问这一句）。 */
    public static boolean isHot(ItemStack stack) {
        return stack.getItem() instanceof Hot;
    }

    /** 标记接口：实现它就自动属于"烧红的金属"这一类。 */
    public interface Hot {
    }
}
