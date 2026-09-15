package cn.blockforge.generated.slashbladereshslashblad;

/** 连续、单调递增且边际递减；不更改原生精炼计数。 */
public final class RefinementCurve {
    private RefinementCurve() {}
    public static double combatBonus(double linearBonus) {
        return 10.0 * Math.log1p(Math.max(0.0, linearBonus) / 10.0);
    }
    public static double soulAttack(int refine) {
        return 5.0 * Math.log1p(Math.max(0, refine) / 20.0);
    }
    public static float nextSoulGain(int refine) {
        int n = Math.max(0, refine);
        return (float) (soulAttack(n == Integer.MAX_VALUE ? n : n + 1) - soulAttack(n));
    }
}
