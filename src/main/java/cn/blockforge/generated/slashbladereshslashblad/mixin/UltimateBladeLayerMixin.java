package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.UltimateJudgementCutClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade",
        "mods.flammpfeil.slashblade.client.renderer.layers.LayerSlashBlade"
}, remap = false)
public abstract class UltimateBladeLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), require = 0)
    private void ultimateBegin(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
            float limb, float amount, float partial, float age, float yaw, float pitch, CallbackInfo ci) {
        UltimateJudgementCutClient.motionEntity = entity.getId();
        UltimateJudgementCutClient.motionPartial = partial;
    }
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("RETURN"), require = 0)
    private void ultimateEnd(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
            float limb, float amount, float partial, float age, float yaw, float pitch, CallbackInfo ci) {
        UltimateJudgementCutClient.motionEntity = -1;
    }
}
