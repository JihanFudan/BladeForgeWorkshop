package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCut;
import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCutClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "jp.nyatla.nymmd.MmdMotionPlayer", remap = false)
public abstract class UltimateMotionMixin {
    @ModifyVariable(method = "updateMotionBonesAndSkinning", at = @At("HEAD"), argsOnly = true)
    private float ultimateTime(float original) {
        // 检视已整体换成网站版的连招管线，由重锋自己喂帧，这里只剩 SSA 上斩一条通道。
        float age = UltimateJudgementCutClient.animationAge(UltimateJudgementCutClient.motionEntity,
                UltimateJudgementCutClient.motionPartial);
        if (age < 0) return original;
        // 重锋 upperslash 的 1600—1659 帧与 upperslash_end 的 1659—1693 帧连续衔接。
        float frame = age < UltimateJudgementCut.SLASH_PHASE_TICKS
                ? 1600.0f + age * 59.0f / UltimateJudgementCut.SLASH_PHASE_TICKS
                : 1659.0f + (age - UltimateJudgementCut.SLASH_PHASE_TICKS) * 34.0f
                    / (UltimateJudgementCut.DURATION_TICKS - UltimateJudgementCut.SLASH_PHASE_TICKS);
        return frame * (1000.0f / 30.0f);
    }
}
