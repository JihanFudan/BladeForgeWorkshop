package cn.blockforge.generated.slashbladereshslashblad.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 在同一张物品贴图上连续降低绿蓝分量，保留纹理、透明边缘与模型轮廓。
 * heat 是烧红的程度（红），orange 是过火的程度：在红的基础上把绿色分量
 * 一路提回来，颜色就从暗红渐变成发光的橙黄。
 */
final class HeatTint implements VertexConsumer {
    private final VertexConsumer target;
    private final float heat;
    private final float orange;

    private HeatTint(VertexConsumer target, float heat, float orange) {
        this.target = target;
        this.heat = heat;
        this.orange = orange;
    }

    static MultiBufferSource wrap(MultiBufferSource source, float heat, float orange) {
        return type -> new HeatTint(source.getBuffer(type), heat, orange);
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        target.addVertex(x, y, z); return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        float greenFactor = (1f - 0.72f * heat) * (1f - orange) + orange;
        float blueFactor = (1f - 0.91f * heat) * (1f - orange) + 0.16f * orange;
        target.setColor(r, Math.round(g * greenFactor), Math.round(b * blueFactor), a);
        return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(x, y, z); return this; }
}
