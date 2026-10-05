package cn.blockforge.generated.slashbladereshslashblad;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 究极次元斩的轻量网络同步：一条触发请求、一条客户端演出通知。 */
public final class UltimateJudgementCutNetwork {
    private UltimateJudgementCutNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(TriggerPayload.TYPE, TriggerPayload.STREAM_CODEC,
                UltimateJudgementCutNetwork::handleTrigger);
        registrar.playToClient(StartPayload.TYPE, StartPayload.STREAM_CODEC,
                UltimateJudgementCutNetwork::handleStart);
        registrar.playToClient(CancelPayload.TYPE, CancelPayload.STREAM_CODEC,
                UltimateJudgementCutNetwork::handleCancel);
        registrar.playToClient(CooldownPayload.TYPE, CooldownPayload.STREAM_CODEC,
                UltimateJudgementCutNetwork::handleCooldown);
    }

    private static void handleTrigger(TriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) UltimateJudgementCut.tryStart(player);
        });
    }

    private static void handleStart(StartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UltimateJudgementCutClient.start(payload));
    }

    private static void handleCancel(CancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UltimateJudgementCutClient.cancel(payload.casterEntityId()));
    }

    private static void handleCooldown(CooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UltimateJudgementCutClient.cooldown(payload.remainingTicks()));
    }

    public static void requestStart() {
        PacketDistributor.sendToServer(TriggerPayload.INSTANCE);
    }

    public static void sendCooldown(ServerPlayer player, int remainingTicks) {
        PacketDistributor.sendToPlayer(player, new CooldownPayload(remainingTicks));
    }

    public static void broadcastCancel(ServerPlayer caster) {
        CancelPayload payload = new CancelPayload(caster.getId());
        PacketDistributor.sendToPlayer(caster, payload);
        PacketDistributor.sendToPlayersTrackingEntity(caster, payload);
    }

    public static void broadcastStart(UltimateJudgementCut.Session session) {
        ServerPlayer caster = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(session.casterId());
        if (caster == null || !(caster.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        StartPayload payload = new StartPayload(caster.getId(), session.casterId(), session.center(),
                session.yaw(), caster.level().getGameTime());
        for (ServerPlayer viewer : level.players()) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    public record TriggerPayload() implements CustomPacketPayload {
        public static final TriggerPayload INSTANCE = new TriggerPayload();
        public static final Type<TriggerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "ultimate_judgement_cut_trigger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CancelPayload(int casterEntityId) implements CustomPacketPayload {
        public static final Type<CancelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "ultimate_judgement_cut_cancel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CancelPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.casterEntityId()),
                        buffer -> new CancelPayload(buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CooldownPayload(int remainingTicks) implements CustomPacketPayload {
        public static final Type<CooldownPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "ultimate_judgement_cut_cooldown"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CooldownPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeVarInt(payload.remainingTicks()),
                buffer -> new CooldownPayload(buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StartPayload(int casterEntityId, UUID casterId, Vec3 center, float yaw, long startGameTime)
            implements CustomPacketPayload {
        public static final Type<StartPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "ultimate_judgement_cut_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.casterEntityId);
                    buffer.writeUUID(payload.casterId);
                    buffer.writeVec3(payload.center);
                    buffer.writeFloat(payload.yaw);
                    buffer.writeVarLong(payload.startGameTime);
                },
                buffer -> new StartPayload(buffer.readVarInt(), buffer.readUUID(), buffer.readVec3(),
                        buffer.readFloat(), buffer.readVarLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
