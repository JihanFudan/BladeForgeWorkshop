package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.RefinementCurve;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 压缩本体已计入等级限制与配置倍率的精炼伤害项，保留 S 级门槛。 */
@Pseudo
@Mixin(targets = "mods.flammpfeil.slashblade.util.AttackHelper", remap = false)
public abstract class RefinementDamageMixin {
    @Redirect(method = "getRankBonus", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"), remap = false)
    private static double diminishingRefine(double rankFloor, double linearRefine) {
        // 原式第二项已包含 min(玩家等级, 精炼数) 和服务器精炼倍率。
        // 对这项做连续压缩，不改变段位底值或最终攻击的其他加成。
        double reduced = RefinementCurve.combatBonus(linearRefine);
        return Math.max(rankFloor, reduced);
    }
}
