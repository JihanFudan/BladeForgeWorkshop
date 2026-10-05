package cn.blockforge.generated.slashbladereshslashblad;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 组装动画的轻量网络：只有一条 S2C 包。
 * 服务端在消耗材料、登记会话后向施法者本人和周围玩家广播开始时刻，
 * 客户端按 BladeAssembly 中同一组刻数常量渲染部件飞行，无需逐刻同步。
 */
public final class BladeAssemblyNetwork {
    private BladeAssemblyNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(StartPayload.TYPE, StartPayload.STREAM_CODEC,
                BladeAssemblyNetwork::handleStart);
    }

    private static void handleStart(StartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> cn.blockforge.generated.slashbladereshslashblad.client
                .BladeAssemblyClient.start(payload));
    }

    public static void broadcastStart(ServerPlayer caster, int kind, int metal, long startGameTime) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(caster,
                new StartPayload(caster.getId(), kind, metal, startGameTime));
    }

    /** kind：0 木偶 / 1 竹光 / 2 寒霜；metal：0 铁 / 1 金 / 2 铜。 */
    public record StartPayload(int entityId, int kind, int metal, long startGameTime)
            implements CustomPacketPayload {
        public static final Type<StartPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                GeneratedMod.MOD_ID, "blade_assembly_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeVarInt(payload.kind());
                    buffer.writeVarInt(payload.metal());
                    buffer.writeVarLong(payload.startGameTime());
                },
                        buffer -> new StartPayload(buffer.readVarInt(), buffer.readVarInt(),
                                buffer.readVarInt(), buffer.readVarLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
