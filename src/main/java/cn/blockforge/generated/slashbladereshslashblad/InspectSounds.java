package cn.blockforge.generated.slashbladereshslashblad;

import java.util.function.Supplier;
import cn.blockforge.generated.slashbladereshslashblad.InspectComboStates.Variant;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 检视音效。金属刀沿用与 SlashBladeInspect（网站版）相同的五个 ogg；
 * 木刀/竹刀改用合成的木质音源（draw/knock/spin/hold 各一套），
 * 听起来是"笃、唰"的空心木击而不是钢刃摩擦——这是用户要求的"更像木头"。
 *
 * 播放时刻由 InspectComboStates 中每把刀自己的时间轴触发，
 * 事件帧与 r9 编排（缓拔前挥砍 / 纵向检视 / 双旋转 / 阎魔推刀甩出）逐一对位，
 * 解决"动画与声音不对齐"。
 */
public final class InspectSounds {
    /** 检视时间轴事件类型。 */
    public enum Cue {
        HOLD, SHED, NUDGE, DRAW, SLASH, SPIN, SHEATHE
    }

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, GeneratedMod.MOD_ID);

//    public static final Supplier<SoundEvent> DRAW = register("draw");
//    public static final Supplier<SoundEvent> QUICKLY_DRAW = register("quickly_draw");
public static final Supplier<SoundEvent> DRAW = register("quickly_draw");
    public static final Supplier<SoundEvent> SPIN = register("spin");
    public static final Supplier<SoundEvent> SHED_SHEATH = register("shed_sheath");
    public static final Supplier<SoundEvent> HOLD = register("hold");

    public static final Supplier<SoundEvent> DRAW_WOOD = register("draw_wood");
    public static final Supplier<SoundEvent> KNOCK_WOOD = register("knock_wood");
    public static final Supplier<SoundEvent> SPIN_WOOD = register("spin_wood");
    public static final Supplier<SoundEvent> HOLD_WOOD = register("hold_wood");
    public static final Supplier<SoundEvent> DRAW_BAMBOO = register("draw_bamboo");
    public static final Supplier<SoundEvent> KNOCK_BAMBOO = register("knock_bamboo");
    public static final Supplier<SoundEvent> SPIN_BAMBOO = register("spin_bamboo");
    public static final Supplier<SoundEvent> HOLD_BAMBOO = register("hold_bamboo");

    private InspectSounds() {
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, name)));
    }

    /** 各刀型的音高倍率（1.0=原版）。 */
    private static float variantPitch(LivingEntity entity) {
        return switch (InspectComboStates.currentVariant(entity)) {
            case WOOD -> 0.82f;     // 木偶
            case BAMBOO -> 1.28f;   // 竹光
            case MURAMASA -> 0.64f; // 村正
            case FOX_BLACK -> 0.78f;// 黑狐
            case FOX_WHITE -> 1.12f;// 白狐
            case YAMATO -> 0.55f;   // 阎魔
            case KOSEKI -> 0.45f;   // 枯石
            case TSKUMO -> 0.60f;   // 付丧
            case SANGE -> 0.62f;    // 散华
            default -> 1.0f;
        };
    }

    private static boolean wooden(LivingEntity entity) {
        Variant variant = InspectComboStates.currentVariant(entity);
        return variant == Variant.WOOD || variant == Variant.BAMBOO;
    }

    private static void play(LivingEntity entity, SoundEvent event, float volume, float basePitch) {
        if (entity.level().isClientSide()) return;
        entity.level().playSound((Player) null, entity.getX(), entity.getY(), entity.getZ(),
                event, SoundSource.PLAYERS, volume, basePitch * variantPitch(entity));
    }

    /** 时间轴入口：按事件类型 + 刀型分发。 */
    public static void playCue(LivingEntity entity, Cue cue) {
        switch (cue) {
            case HOLD -> playHold(entity);
            case SHED -> playShedSheath(entity);
            case NUDGE -> playEnmaNudge(entity);
            case DRAW -> playDraw(entity);
            case SLASH -> playSlash(entity);
            case SPIN -> playSpin(entity);
            case SHEATHE -> playSheathe(entity);
        }
    }

    public static void playHold(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[3], 1.0F, 1.0F);
        } else {
            play(entity, HOLD.get(), 1.0F, 1.0F);
        }
    }

    public static void playQuicklyDraw(LivingEntity entity) {
        play(entity, DRAW.get(), 0.9F, 1.0F);
    }

    public static void playDraw(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[0], 1.0F, 1.0F);
        } else {
            playQuicklyDraw(entity);
        }
    }

    /** 挥砍/甩出的风声：金属刀用高频 spin 音，木刀用双 whoosh 木音。 */
    public static void playSlash(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[2], 0.95F, 1.08F);
        } else {
            play(entity, SPIN.get(), 0.9F, 1.25F);
        }
    }

    public static void playSpin(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[2], 0.85F, 0.95F);
        } else {
            play(entity, SPIN.get(), 0.8F, 0.65F);
        }
    }

    /** 出鞘摩擦声（SHED）：加速播放，音调 1.5 倍，时长缩短约 1/3。 */
    public static void playShedSheath(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[1], 1.0F, 1.5F);
        } else {
            play(entity, SHED_SHEATH.get(), 1.0F, 1.5F);
        }
    }

    /** 收刀入鞘：金属刀用原版"咔哒"，木刀/竹刀换成木击。 */
    public static void playSheathe(LivingEntity entity) {
        if (wooden(entity)) {
            play(entity, woodSet(entity)[1], 1.1F, 1.15F);
        } else {
            AttackManager.playQuickSheathSoundAction(entity);
        }
    }

    /** 阎魔顶出（左手把刀从鞘口顶出一截）：出鞘摩擦声同样加速。 */
    public static void playEnmaNudge(LivingEntity entity) {
        play(entity, SHED_SHEATH.get(), 0.85F, 1.35F);
    }

    /** [draw, knock, spin, hold] 四件套，按 WOOD/BAMBOO 选择。 */
    private static SoundEvent[] woodSet(LivingEntity entity) {
        return InspectComboStates.currentVariant(entity) == Variant.BAMBOO
                ? new SoundEvent[]{DRAW_BAMBOO.get(), KNOCK_BAMBOO.get(), SPIN_BAMBOO.get(), HOLD_BAMBOO.get()}
                : new SoundEvent[]{DRAW_WOOD.get(), KNOCK_WOOD.get(), SPIN_WOOD.get(), HOLD_WOOD.get()};
    }
}
