package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.InspectComboStates;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 检视期间隐藏第一人称玩家肢体：原版第一人称手臂（renderRightHand/renderLeftHand）
 * 会在刀前面盖一层皮肤模型。用户要求"第一人称不需要显示玩家肢体"，
 * 这里在检视连招激活时直接取消两只手臂的绘制（刀与鞘由接管渲染器绘制）；
 * 第三人称与截图渲染不受影响。
 */
@Mixin(PlayerRenderer.class)
public abstract class InspectArmHideMixin {
    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void hideInspectRightArm(PoseStack pose, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, CallbackInfo ci) {
        hideIfInspecting(ci);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void hideInspectLeftArm(PoseStack pose, MultiBufferSource buffers, int light,
                                    AbstractClientPlayer player, CallbackInfo ci) {
        hideIfInspecting(ci);
    }

    private static void hideIfInspecting(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                && InspectComboStates.isInspecting(minecraft.player)) {
            ci.cancel();
        }
    }
}
