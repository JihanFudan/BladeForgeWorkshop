package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.SlashBladeBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** 使用拔刀剑原始 OBJ 的 blade / sheath 分组，分别平放在铁砧台面。 */
final class BladeOnAnvil {
    private static final Method DEFAULT_MODEL;
    private static final Method DEFAULT_TEXTURE;
    private static final Method GET_MODEL;
    private static final Method RENDER;
    private static final Method LUMINOUS;
    private static final Object MANAGER;
    static {
        try {
            Class<?> renderer = Class.forName("mods.flammpfeil.slashblade.client.renderer.SlashBladeTEISR");
            DEFAULT_MODEL = renderer.getMethod("stackDefaultModel", ItemStack.class);
            DEFAULT_TEXTURE = renderer.getMethod("stackDefaultTexture", ItemStack.class);
            Class<?> manager = Class.forName("mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager");
            MANAGER = manager.getMethod("getInstance").invoke(null);
            GET_MODEL = manager.getMethod("getModel", ResourceLocation.class);
            Class<?> wavefront = Class.forName("mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject");
            Class<?> renderState = Class.forName("mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState");
            Class<?>[] args = {ItemStack.class, wavefront, String.class, ResourceLocation.class,
                    PoseStack.class, MultiBufferSource.class, int.class};
            RENDER = renderState.getMethod("renderOverrided", args);
            LUMINOUS = renderState.getMethod("renderOverridedLuminous", args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("拔刀剑模型接口不匹配", e);
        }
    }

    static void draw(ItemStack stack, double y, PoseStack pose, MultiBufferSource buffers, int light) {
        CompoundTag state = SlashBladeBridge.read(stack);
        try {
            Object itemRenderer = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer();
            ResourceLocation modelId = state.contains("ModelName") ? ResourceLocation.parse(state.getString("ModelName"))
                    : (ResourceLocation) DEFAULT_MODEL.invoke(itemRenderer, stack);
            ResourceLocation texture = state.contains("TextureName") ? ResourceLocation.parse(state.getString("TextureName"))
                    : (ResourceLocation) DEFAULT_TEXTURE.invoke(itemRenderer, stack);
            Object model = GET_MODEL.invoke(MANAGER, modelId);
            part(stack, model, texture, state.getBoolean("isBroken") ? "blade_damaged" : "blade", 0.42, y, pose, buffers, light);
            part(stack, model, texture, "sheath", 0.64, y, pose, buffers, light);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法展示拔刀剑原始模型", e);
        }
    }

    private static void part(ItemStack stack, Object model, ResourceLocation texture, String group, double z,
                             double y, PoseStack pose, MultiBufferSource buffers, int light) throws ReflectiveOperationException {
        pose.pushPose();
        try {
            pose.translate(0.5, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.0025f, 0.0025f, 0.0025f);
            pose.translate(130, 0, 0);
            RENDER.invoke(null, stack, model, group, texture, pose, buffers, light);
            LUMINOUS.invoke(null, stack, model, group + "_luminous", texture, pose, buffers, light);
        } finally { pose.popPose(); }
    }
    private BladeOnAnvil() {}
}
