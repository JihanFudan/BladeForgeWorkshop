package cn.blockforge.generated.slashbladereshslashblad;

import java.util.Objects;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 检视网络：与网站版 SlashBladeInspect 同构——只有一条 C2S 包。
 * 客户端按 G 选好检视动作（现为唯一的带拔刀一段）后发到此包，服务端复核"主手确实是拔刀剑"
 * 再调用重锋的 updateComboSeq 设连招状态；之后的画面与时间线全部由重锋
 * 自身的运行时数据同步驱动（连招状态、动作起点随刀的运行时数据下发），
 * 无需任何自定义 S2C 广播。
 */
public final class BladeInspectNetwork {
    private BladeInspectNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(SetComboPayload.TYPE, SetComboPayload.STREAM_CODEC,
                BladeInspectNetwork::handleSetCombo);
        registrar.playToServer(CancelInspectPayload.TYPE, CancelInspectPayload.STREAM_CODEC,
                BladeInspectNetwork::handleCancelInspect);
    }

    private static void handleSetCombo(SetComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack blade = player.getMainHandItem();
            if (!(blade.getItem() instanceof ItemSlashBlade)) return;
            // 拔刀剑技能释放中不允许发起检视（服务端兜底）。
            if (InspectComboStates.isSkillInUse(blade)) return;
            // 服务端按真实主手刀重新选择动作，不能由客户端伪造刀种或任意连招。
            if (!InspectComboStates.isInspectId(payload.combo())) return;
            ResourceLocation selectedCombo = InspectComboStates.inspectId(blade);
            // 究极次元斩咏唱中：与旧版行为一致，检视优先，先打断 SSA。
            UltimateJudgementCut.interrupt(player);
            BladeStateAccess.of(blade)
                    .ifPresent(state -> state.updateComboSeq(player, selectedCombo));
            // 跟踪这把刀：一旦切手/死亡/旁观就强制清零，杜绝"续播"。
            InspectInterruptGuard.track(player, blade);
        });
    }

    private static void handleCancelInspect(CancelInspectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            InspectInterruptGuard.cancel(player);
        });
    }

    public static void requestSetCombo(ResourceLocation combo) {
        PacketDistributor.sendToServer(new SetComboPayload(combo));
    }

    public static void requestCancelInspect() {
        PacketDistributor.sendToServer(new CancelInspectPayload());
    }

    public record SetComboPayload(ResourceLocation combo) implements CustomPacketPayload {
        public static final Type<SetComboPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "blade_inspect_set_combo"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetComboPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> buffer.writeResourceLocation(payload.combo()),
                        buffer -> new SetComboPayload(buffer.readResourceLocation()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public SetComboPayload {
            Objects.requireNonNull(combo, "combo");
        }
    }

    /** C2S：玩家主动打断检视（再次按 G 或打开背包），服务端立即清零连招与音效。 */
    public record CancelInspectPayload() implements CustomPacketPayload {
        public static final Type<CancelInspectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "blade_inspect_cancel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CancelInspectPayload> STREAM_CODEC =
                StreamCodec.unit(new CancelInspectPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
