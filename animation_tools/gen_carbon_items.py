# -*- coding: utf-8 -*-
"""
gen_carbon_items.py —— 生成 1.0.4-r17 新物品的 16x16 背包图标。

三件冷料 + 两件灼热料，全部从现有图标按调色板派生，形状/轮廓与原钢锭完全一致，
只换色相与点缀，这样放进背包格子不会显得"画风不同"：

  low_carbon_steel.png   ← steel_ingot.png   偏冷的亮蓝灰（低碳钢更软更亮）
  high_carbon_steel.png  ← steel_ingot.png   偏暗的中灰 + 碳黑斑点 + 淡蓝回火纹
  heated_low_carbon.png  ← heated_steel.png  亮黄白（烧透的低碳钢，温度看着更高）
  heated_high_carbon.png ← heated_steel.png  深橙红 + 暗红碳斑（含碳多，颜色发闷）
  carbon_powder.png      手绘：一小堆炭黑粉末（颗粒 + 外散几粒）

用法: python3 animation_tools/gen_carbon_items.py
"""
import colorsys
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "item")


def recolor(src, hue, sat, v_scale, v_clamp=None, jitter=0.02, seed=0, keep_outline=True):
    """按母图像素一一对应地重染：色相统一到 hue、饱和按母图明暗缩放、
    明度乘 v_scale。母图里最暗的那档（轮廓线）默认原样保留，免得描边被洗掉。"""
    img = Image.open(os.path.join(ITEM_DIR, src)).convert("RGBA")
    px = img.load()
    rnd = random.Random(seed)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a < 128:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if keep_outline and v <= 0.20:
                continue
            nv = v * v_scale
            if v_clamp is not None:
                nv = min(nv, v_clamp)
            nv = max(0.0, min(1.0, nv))
            nh = (hue + rnd.uniform(-jitter, jitter)) % 1.0
            ns = max(0.0, min(1.0, sat * (0.55 + 0.45 * v) + rnd.uniform(-0.05, 0.05)))
            nr, ng, nb = colorsys.hsv_to_rgb(nh, ns, nv)
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img


def flecks(img, hue, sat, vmin, vmax, count, seed, size=1):
    """在已有像素上撒几块小斑（碳斑、火色），不越出母图轮廓。

    size=1 的十字是 5 格，放在 16 格的锭子上太抢眼（第一版高碳钢的黑斑
    就成了一个个黑十字），所以画完再随机缺掉几格，变成毛边小团。"""
    px = img.load()
    rnd = random.Random(seed)
    spots = [(x, y) for y in range(img.height) for x in range(img.width) if px[x, y][3] >= 128]
    for _ in range(count):
        cx, cy = rnd.choice(spots)
        col = colorsys.hsv_to_rgb((hue + rnd.uniform(-0.02, 0.02)) % 1.0, sat, rnd.uniform(vmin, vmax))
        col = tuple(int(c * 255) for c in col)
        for dy in range(-size, size + 1):
            for dx in range(-size, size + 1):
                if dx * dx + dy * dy > size * size + 0.5:
                    continue
                if size and rnd.random() < 0.45:      # 毛边：十字偶尔缺几格
                    continue
                x, y = cx + dx, cy + dy
                if 0 <= x < img.width and 0 <= y < img.height and px[x, y][3] >= 128:
                    px[x, y] = (*col, 255)
    return img


def save(img, name):
    out = os.path.join(ITEM_DIR, name + ".png")
    img.save(out)
    print("  ->", os.path.relpath(out, ROOT))


def carbon_powder():
    """手绘碳粉：中间一堆扁圆的粉末，边缘颗粒毛糙，外圈散几粒。

    第一版只画到明度 0.32，放进深灰的背包格子里几乎看不见（渲图自查发现的）。
    炭黑本身不反光，所以整体仍压在暗档，但堆顶给一排明显的亮颗粒、
    堆形放大半格并描一圈极淡的冷灰边，靠"轮廓 + 顶光"读出来是粉末堆。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(7)
    cx, cy, rx, ry = 8.0, 10.0, 6.0, 3.6
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
            d += (rnd.random() - 0.5) * 0.30          # 边缘打毛，不做成正椭圆
            if d > 1.0:
                continue
            # 堆顶偏亮、堆脚偏暗：一点点自上而下的体积感
            t = 1.0 - abs(y - (cy - 1.6)) / 5.5
            v = 0.16 + 0.26 * max(0.0, min(1.0, t)) + rnd.uniform(-0.02, 0.04)
            h = 0.62 + rnd.uniform(-0.04, 0.04)       # 极淡的冷灰蓝，避免死黑
            r, g, b = colorsys.hsv_to_rgb(h, 0.14, v)
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    # 轮廓描边：堆形外圈那格压成暗边，内侧紧接一排亮颗粒，剪影才立得住
    for y in range(16):
        for x in range(16):
            if px[x, y][3] < 128:
                continue
            if any(px[x + dx, y + dy][3] < 128
                   for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))
                   if 0 <= x + dx < 16 and 0 <= y + dy < 16):
                r, g, b = colorsys.hsv_to_rgb(0.62, 0.12, 0.09)
                px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    # 高光颗粒：堆顶那几颗反一点光，才读得出是"粉末"而不是一滩墨
    top = [(x, y) for y in range(6, 10) for x in range(3, 13) if px[x, y][3] >= 128]
    for _ in range(11):
        x, y = rnd.choice(top)
        v = rnd.uniform(0.46, 0.62)
        r, g, b = colorsys.hsv_to_rgb(0.6, 0.08, v)
        px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    # 外散的几粒（只左右两侧，别掉到下面变成噪点）
    for _ in range(5):
        side = rnd.choice((-1, 1))
        x = int(round(cx + side * rnd.uniform(6.4, 7.6)))
        y = int(round(cy + rnd.uniform(-1.4, 1.4)))
        if 0 <= x < 16 and 0 <= y < 16:
            v = rnd.uniform(0.20, 0.34)
            r, g, b = colorsys.hsv_to_rgb(0.62, 0.12, v)
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    return img


def main():
    # ---- 冷料 ----
    low = recolor("steel_ingot.png", hue=0.575, sat=0.30, v_scale=1.30, v_clamp=0.86, seed=11)
    low = flecks(low, 0.55, 0.10, 0.88, 0.96, 4, seed=21)          # 亮白反光点
    save(low, "low_carbon_steel")

    high = recolor("steel_ingot.png", hue=0.66, sat=0.20, v_scale=0.80, seed=12)
    high = flecks(high, 0.0, 0.0, 0.06, 0.13, 5, seed=31)          # 碳黑斑点
    high = flecks(high, 0.60, 0.35, 0.42, 0.52, 3, seed=41)        # 淡蓝回火纹
    save(high, "high_carbon_steel")

    # ---- 灼热料（母图 heated_steel.png 已是 heated 调色，只重染色相/明度）----
    # 低碳钢：比钢锭更"烧透"，偏白但保留黄橙底，否则看着像奶酪而不是热金属
    hot_low = recolor("heated_steel.png", hue=0.09, sat=0.62, v_scale=1.02, seed=13, keep_outline=False)
    hot_low = flecks(hot_low, 0.11, 0.30, 0.92, 1.0, 3, seed=51)   # 偏白热区
    hot_low = flecks(hot_low, 0.05, 0.85, 0.95, 1.0, 4, seed=52)    # 橙黄火苗边
    save(hot_low, "heated_low_carbon")

    # 高碳钢：含碳多，颜色发闷发红，暗斑用暗红而不是近黑（近黑在橙底上像窟窿）
    hot_high = recolor("heated_steel.png", hue=0.026, sat=0.92, v_scale=0.90, seed=14, keep_outline=False)
    hot_high = flecks(hot_high, 0.0, 0.80, 0.30, 0.42, 5, seed=61)  # 暗红碳斑
    hot_high = flecks(hot_high, 0.07, 0.85, 0.92, 1.0, 3, seed=62)  # 几粒火星
    save(hot_high, "heated_high_carbon")

    save(carbon_powder(), "carbon_powder")
    print("OK: 5 张新图标")


if __name__ == "__main__":
    main()
