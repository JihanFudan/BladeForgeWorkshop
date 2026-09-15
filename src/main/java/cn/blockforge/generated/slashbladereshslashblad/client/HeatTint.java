package cn.blockforge.generated.slashbladereshslashblad.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

/** 在同一张物品贴图上连续降低绿蓝分量，保留纹理、透明边缘与模型轮廓。 */
final class HeatTint implements VertexConsumer {
    private final VertexConsumer target;
    private final float heat;

    private HeatTint(VertexConsumer target, float heat) {
        this.target = target;
        this.heat = heat;
    }

    static MultiBufferSource wrap(MultiBufferSource source, float heat) {
        return type -> new HeatTint(source.getBuffer(type), heat);
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        target.addVertex(x, y, z); return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        target.setColor(r, Math.round(g * (1f - 0.72f * heat)),
                Math.round(b * (1f - 0.91f * heat)), a);
        return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(x, y, z); return this; }
}
