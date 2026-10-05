package cn.blockforge.generated.slashbladereshslashblad;

import java.util.List;
import java.util.Locale;
import cn.blockforge.generated.slashbladereshslashblad.InspectSounds.Cue;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本检视系统灵感来自于 猫叩Mewkooo 项目 SlashBlade Inspect，
 * 在此特别感谢原作者与其他贡献者的支持。
 * 检视连招状态，每把刀独立注册检视动作与
 * 独立动画文件（combostate/inspect_*.vmd），方便逐刀修改：
 *   inspect_a.vmd          通用（未命名刀/其他）
 *   inspect_wood.vmd       木刀
 *   inspect_bamboo.vmd     竹刀
 *   inspect_muramasa.vmd   村正
 *   inspect_fox_black.vmd  黑狐
 *   inspect_fox_white.vmd  白狐
 *   inspect_yamato.vmd     阎魔
 * 改某一把刀的音效只改对应数组。
 * 打断规则：普通攻击可以打断检视；技能类攻击受优先级限制不能被检视顶掉，同样打断不了检视。
 * 全部动作都没有伤害判定，超时后回到 none。
 */
public final class InspectComboStates {
    public static final DeferredRegister<ComboState> COMBO_STATE =
            DeferredRegister.create(ComboState.REGISTRY_KEY, GeneratedMod.MOD_ID);

    /** 音效调用说明
     * Cue.HOLD=握鞘
     * Cue.NUDGE=顶刀
     * Cue.DRAW=拔刀
     * Cue.SPIN=回旋
     * Cue.SLASH=挥砍
     * Cue.SHEATHE=收刀
     * Cue.SHED=摩擦（原作者的音效我暂时没用上）
     * */

    /** 阎魔专属音效时间轴 */
    private static final Object[] YAMATO_CUES = {
            5, Cue.HOLD, 19, Cue.NUDGE, 33, Cue.DRAW, 65, Cue.NUDGE,
            85, Cue.NUDGE, 126, Cue.SPIN, 151, Cue.SPIN, 185, Cue.SHEATHE
    };

    /** 通用音效时间轴 */
    private static final Object[] GENERIC_CUES = {
            5, Cue.HOLD, 20, Cue.NUDGE, 30, Cue.DRAW, 73, Cue.NUDGE,
            108, Cue.NUDGE, 136, Cue.SPIN, 185, Cue.SHEATHE
    };

    /** 木刀音效时间轴 */
    private static final Object[] WOOD_CUES = {
            5, Cue.HOLD, 20, Cue.NUDGE, 30, Cue.DRAW, 67, Cue.NUDGE,
            90, Cue.NUDGE, 112, Cue.NUDGE, 146, Cue.SPIN, 185, Cue.SHEATHE
    };

    /** 竹刀音效时间轴(原设定成废案，竹刀继承木刀音效与动画) */
    private static final Object[] BAMBOO_CUES = WOOD_CUES;
    /** 村正音效时间轴 */
    private static final Object[] MURAMASA_CUES = {
            5, Cue.HOLD, 20, Cue.NUDGE, 23, Cue.DRAW, 71, Cue.NUDGE,
            101, Cue.SLASH, 115, Cue.SPIN, 136, Cue.SPIN, 185, Cue.SHEATHE
    };
    /** 黑狐音效时间轴 */
    private static final Object[] FOX_BLACK_CUES = {
            5, Cue.HOLD, 20, Cue.NUDGE, 23, Cue.DRAW, 63, Cue.SLASH,
            90, Cue.NUDGE, 102, Cue.SPIN, 150, Cue.SPIN, 185, Cue.SHEATHE
    };
    /** 白狐音效时间轴 */
    private static final Object[] FOX_WHITE_CUES = {
            5, Cue.HOLD, 20, Cue.NUDGE, 23, Cue.DRAW, 63, Cue.SLASH,
            116, Cue.NUDGE, 138, Cue.SPIN, 185, Cue.SHEATHE
    };
    /** 枯石音效时间轴（暂用村正的音效） */
    private static final Object[] KOSEKI_CUES = MURAMASA_CUES;
    /** 付丧音效时间轴（暂用村正的音效） */
    private static final Object[] TSKUMO_CUES = MURAMASA_CUES;
    /** 散华音效时间轴（暂用阎魔的音效） */
    private static final Object[] SANGE_CUES = YAMATO_CUES;

    // 下面是注册动画与音效的
    public static final DeferredHolder<ComboState, ComboState> INSPECT_A =
            register("inspect_a", "inspect_a", GENERIC_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_WOOD =
            register("inspect_wood", "inspect_wood", WOOD_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_BAMBOO =
            register("inspect_bamboo", "inspect_bamboo", BAMBOO_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_MURAMASA =
            register("inspect_muramasa", "inspect_muramasa", MURAMASA_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_FOX_BLACK =
            register("inspect_fox_black", "inspect_fox_black", FOX_BLACK_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_FOX_WHITE =
            register("inspect_fox_white", "inspect_fox_white", FOX_WHITE_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_YAMATO =
            register("inspect_yamato", "inspect_yamato", YAMATO_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_KOSEKI =
            register("inspect_koseki", "inspect_koseki", KOSEKI_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_TSKUMO =
            register("inspect_tsukumo", "inspect_tsukumo", TSKUMO_CUES);
    public static final DeferredHolder<ComboState, ComboState> INSPECT_SANGE =
            register("inspect_sange", "inspect_sange", SANGE_CUES);

    private static final List<ResourceLocation> INSPECT_IDS = List.of(
            id("inspect_a"), id("inspect_wood"), id("inspect_bamboo"), id("inspect_muramasa"),
            id("inspect_fox_black"), id("inspect_fox_white"), id("inspect_yamato"),
            id("inspect_koseki"), id("inspect_tsukumo"), id("inspect_sange"));

    private InspectComboStates() {
    }

    /** cues 为 {帧号, Cue, 帧号, Cue, ...} 交替数组；帧→刻换算与动画播放同一时基。 */
    private static DeferredHolder<ComboState, ComboState> register(String id, String motion, Object... cues) {
        ComboState.TimeLineTickAction.TimeLineTickActionBuilder builder =
                ComboState.TimeLineTickAction.getBuilder();
        for (int i = 0; i + 1 < cues.length; i += 2) {
            int frame = (Integer) cues[i];
            Cue cue = (Cue) cues[i + 1];
            int tick = Math.max(0, (int) TimeValueHelper.getTicksFromFrames(frame) - 1);
            builder.put(tick, entity -> InspectSounds.playCue(entity, cue));
        }
        return COMBO_STATE.register(id,
                ComboState.Builder.newInstance()
                        .startAndEnd(0, 202)
                        .priority(50)
                        // 检视动画 202 帧 ≈ 134.7 刻，播完立即超时回到 none，
                        // 结束检视状态，避免 comboSeq 长时间悬挂导致玩家动画被误冻结。
                        .timeout((int) TimeValueHelper.getTicksFromFrames(202) + 1)
                        .motionLoc(ResourceLocation.fromNamespaceAndPath(
                                GeneratedMod.MOD_ID, "combostate/" + motion + ".vmd"))
                        .next(entity -> ComboStateRegistry.COMBO_A1.getId())
                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                        .addTickAction(builder.build())
                        ::build);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, path);
    }

    /** 根据物品 id、命名刀翻译键和模型资源共同选择动作；未列出的刀使用通用动作（＝黑狐）。 */
    public static ResourceLocation inspectId(ItemStack blade) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(blade.getItem());
        if (SlashBlade.prefix("slashblade_wood").equals(itemId)) return INSPECT_WOOD.getId();
        if (SlashBlade.prefix("slashblade_bamboo").equals(itemId)) return INSPECT_BAMBOO.getId();

        String identity = BladeStateAccess.of(blade).map(state -> String.join(" ",
                        state.getTranslationKey(),
                        state.getModel().map(ResourceLocation::toString).orElse(""),
                        state.getTexture().map(ResourceLocation::toString).orElse("")))
                .orElse("")
                .toLowerCase(Locale.ROOT);
        // 名刀·寒霜：检视动画直接用阎魔（yamato）的那一套
        if (containsAny(identity, "frost")) return INSPECT_YAMATO.getId();
        if (containsAny(identity, "muramasa")) return INSPECT_MURAMASA.getId();
        // 散华本体（贴图 sange.png / 模型 named/sange/sange，区别于黑狐白狐的 sange/black、sange/white）
        if (containsAny(identity, "sange") && !containsAny(identity, "black", "white")) return INSPECT_SANGE.getId();
        if (containsAny(identity, "koseki")) return INSPECT_KOSEKI.getId();
        if (containsAny(identity, "tukumo")) return INSPECT_TSKUMO.getId();
        if (containsAny(identity, "fox_black", "sange/black", "fox.black")) return INSPECT_FOX_BLACK.getId();
        if (containsAny(identity, "fox_white", "sange/white", "fox.white")) return INSPECT_FOX_WHITE.getId();
        if (containsAny(identity, "yamato")) return INSPECT_YAMATO.getId();
        if (containsAny(identity, "slashblade_wood", "model/wood.png")) return INSPECT_WOOD.getId();
        if (containsAny(identity, "slashblade_bamboo", "model/bamboo.png")) return INSPECT_BAMBOO.getId();
        return INSPECT_A.getId();
    }

    private static boolean containsAny(String value, String... markers) {
        for (String marker : markers) {
            if (value.contains(marker)) return true;
        }
        return false;
    }

    /** 网络包白名单：只允许把连招设成已注册的检视动作。 */
    public static boolean isInspectId(ResourceLocation loc) {
        return INSPECT_IDS.contains(loc);
    }

    /** 主手刀是否正在释放拔刀剑技能（非空闲、非检视的活动连招）。 */
    public static boolean isSkillInUse(ItemStack blade) {
        return BladeStateAccess.of(blade)
                .map(state -> {
                    ResourceLocation seq = state.getComboSeq();
                    if (seq == null) return false;
                    if (ComboStateRegistry.NONE.getId().equals(seq)) return false;
                    if (ComboStateRegistry.STANDBY.getId().equals(seq)) return false;
                    return !isInspectId(seq);
                })
                .orElse(false);
    }

    /** 当前主手刀是否正处在任意检视连招里。 */
    public static boolean isInspecting(LivingEntity entity) {
        return BladeStateAccess.of(entity.getMainHandItem())
                .map(state -> isInspectId(state.getComboSeq()))
                .orElse(false);
    }

    /**
     * 当前正在检视的刀型（用于分刀型的手臂姿势与音效音高）。
     * 未处于检视连招时返回 {@link Variant#GENERIC}。
     */
    public static Variant currentVariant(LivingEntity entity) {
        return BladeStateAccess.of(entity.getMainHandItem())
                .map(state -> variantOf(state.getComboSeq()))
                .orElse(Variant.GENERIC);
    }

    private static Variant variantOf(ResourceLocation combo) {
        if (combo.equals(INSPECT_WOOD.getId())) return Variant.WOOD;
        if (combo.equals(INSPECT_BAMBOO.getId())) return Variant.BAMBOO;
        if (combo.equals(INSPECT_MURAMASA.getId())) return Variant.MURAMASA;
        if (combo.equals(INSPECT_FOX_BLACK.getId())) return Variant.FOX_BLACK;
        if (combo.equals(INSPECT_FOX_WHITE.getId())) return Variant.FOX_WHITE;
        if (combo.equals(INSPECT_YAMATO.getId())) return Variant.YAMATO;
        if (combo.equals(INSPECT_KOSEKI.getId())) return Variant.KOSEKI;
        if (combo.equals(INSPECT_TSKUMO.getId())) return Variant.TSKUMO;
        if (combo.equals(INSPECT_SANGE.getId())) return Variant.SANGE;
        return Variant.GENERIC;
    }

    public enum Variant {
        GENERIC, WOOD, BAMBOO, MURAMASA, FOX_BLACK, FOX_WHITE, YAMATO, KOSEKI, TSKUMO, SANGE
    }
}
