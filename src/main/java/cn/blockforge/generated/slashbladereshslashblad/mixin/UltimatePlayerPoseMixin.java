package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.BladeAssembly;
import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCut;
import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCutClient;
import cn.blockforge.generated.slashbladereshslashblad.client.BladeAssemblyClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 终极居合（次元斩）与刀条组装期间的玩家手臂配合姿势。
 * 检视期间的玩家手臂操作已按要求移除：检视时玩家恢复原版默认
 * （自然下垂 + 原版摆动 + 正常持刀姿势），刀由接管渲染器独立驱动。
 */
@Mixin(HumanoidModel.class)
public abstract class UltimatePlayerPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void ultimatePose(LivingEntity entity, float limb, float amount, float ticks, float yaw, float pitch, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;

        // 组装动画：右手平举托住悬浮刀条，左手向部件"拧"两下，收尾时自然放下。
        float assemblyAge = BladeAssemblyClient.animationAge(entity.getId(), ticks - entity.tickCount);
        if (assemblyAge >= 0) {
            float lift = Math.min(1.0f, assemblyAge / 8.0f);
            float lower = Math.max(0.0f, Math.min(1.0f, (assemblyAge - BladeAssembly.SHEATHE_SNAP_TICK)
                    / (float) (BladeAssembly.DURATION_TICKS - BladeAssembly.SHEATHE_SNAP_TICK)));
            float hold = lift * (1.0f - lower);
            model.rightArm.xRot = -0.2f - 1.3f * hold;
            model.rightArm.yRot = -0.15f * hold;
            model.rightArm.zRot = -0.1f * hold;
            model.leftArm.xRot = -0.35f - 0.75f * hold;
            model.leftArm.yRot = 0.55f * hold;
            model.body.yRot = 0.0f;
            model.rightLeg.xRot = model.leftLeg.xRot = 0;
            return;
        }

        float age = UltimateJudgementCutClient.animationAge(entity.getId(), ticks - entity.tickCount);
        if (age < 0) return;
        // 上斩从低位向上挥刀，身体保持原朝向；只有手臂缓慢收刀。
        float cut = Math.min(1, age / (float) UltimateJudgementCut.SLASH_PHASE_TICKS);
        float sheath = Math.max(0, Math.min(1,
                (age - UltimateJudgementCut.SLASH_PHASE_TICKS)
                        / (float) (UltimateJudgementCut.DURATION_TICKS - UltimateJudgementCut.SLASH_PHASE_TICKS)));
        model.rightArm.xRot = -0.39f - cut * 2.05f + sheath * 2.59f;
        model.rightArm.yRot = -0.92f + cut * 0.38f - sheath * 1.05f;
        model.rightArm.zRot = -0.24f + cut * 0.38f - sheath * 0.55f;
        model.leftArm.xRot = -0.72f + cut * 0.25f - sheath * 0.25f;
        model.leftArm.yRot = 0.66f - sheath * 0.54f;
        model.body.yRot = 0.0f;
        model.rightLeg.xRot = model.leftLeg.xRot = 0;
    }
}
