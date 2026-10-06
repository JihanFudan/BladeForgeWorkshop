package cn.blockforge.generated.slashbladereshslashblad;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** 对接 1.21.1 前置模组公开的状态入口；通过公开接口调用，避免访问其非公开实现类。 */
public final class SlashBladeBridge {
    private static final Method ACCESS;
    private static final Method SAVE;
    private static final Method LOAD;
    private static final Method SET_SLASH_ART;
    static {
        try {
            Class<?> access = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess");
            Class<?> state = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState");
            ACCESS = access.getMethod("of", ItemStack.class);
            SAVE = state.getMethod("serializeNBT");
            LOAD = state.getMethod("deserializeNBT", CompoundTag.class);
            SET_SLASH_ART = state.getMethod("setSlashArtsKey", net.minecraft.resources.ResourceLocation.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("需要支持 BladeStateAccess 的 1.21.1 版 SlashBlade Resharped", e);
        }
    }

    private static Object state(ItemStack stack) {
        try {
            return ((Optional<?>) ACCESS.invoke(null, stack)).orElseThrow(
                    () -> new IllegalArgumentException("物品不是拔刀剑"));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法访问拔刀剑状态", e);
        }
    }

    public static boolean isBlade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try { return ((Optional<?>) ACCESS.invoke(null, stack)).isPresent(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("无法识别拔刀剑状态", e); }
    }

    public static CompoundTag read(ItemStack stack) {
        try { return (CompoundTag) SAVE.invoke(state(stack)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("无法读取拔刀剑状态", e); }
    }

    public static void write(ItemStack stack, CompoundTag tag) {
        try { LOAD.invoke(state(stack), tag); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("无法写入拔刀剑状态", e); }
    }

    public static void setSlashArt(ItemStack stack, net.minecraft.resources.ResourceLocation slashArt) {
        try { SET_SLASH_ART.invoke(state(stack), slashArt); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("无法写入拔刀剑 SA", e); }
    }

    public static void broadcastMotion(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.resources.ResourceLocation combo, long time) {
        try {
            Class<?> type = Class.forName("mods.flammpfeil.slashblade.event.BladeMotionEvent");
            net.neoforged.bus.api.Event event = (net.neoforged.bus.api.Event) type
                    .getConstructor(net.minecraft.world.entity.LivingEntity.class,
                            net.minecraft.resources.ResourceLocation.class, long.class)
                    .newInstance(entity, combo, time);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法同步拔刀动作", e);
        }
    }

    private SlashBladeBridge() {}
}
