# -*- coding: utf-8 -*-
"""
gen_hot_sheets.py —— 生成"烧红金属"的差异化材质表。

用户反馈：烧红铁锭 / 烧红钢锭 / 烧红融合钢（以及三种烧红刀条）现在共用同一张
材质表、只靠乘色区分，颜色几乎一模一样"看不出区别"。本脚本从现有母表派生出
各自有辨识度的表：

  锭子（16x16，母表 sb_hot.png）：
    sb_hot_iron.png       深橙红 + 边缘焦黑          —— 铁
    sb_hot_steel.png      亮黄白、高亮度低饱和      —— 钢
    sb_hot_fused.png      红紫底 + 蓝紫斜纹 + 火星  —— 融合钢
    sb_hot_lowcarbon.png  近白的亮黄白 + 白热片      —— 低碳钢（r17）
    sb_hot_highcarbon.png 深橙红 + 暗红碳斑 + 火星  —— 高碳钢（r17）

  刀条（320x128，母表 sb_oodachi.png 的刀刃区）：
    sb_hot_blade.png  明亮橙红（正烧透的刀条）
    sb_hot_crude.png  暗红 + 黑斑（粗制、热度发闷）
    sb_hot_clay.png   橙红底 + 黏土黄斑块（覆土刀条）

派生方式保留母表的明暗结构（顶亮底暗的光照带、刀刃的锻纹），只重映射色相/
饱和/明度并叠加少量图案，因此与 AssemblyStageModel 的 UV 完全兼容。

用法: python3 animation_tools/gen_hot_sheets.py
"""
import colorsys
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
TEX_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "assembly")


def recolor(img, hue, sat, v_lo, v_hi, keep_dark=0.18, seed=0, dark_v=None):
    """按母表亮度重映射颜色：亮度 v 映到 [v_lo, v_hi]，色相/饱和统一并带轻微
    逐像素抖动（保留母表的锻纹/杂色，不至于整块塑料感）。
    keep_dark 以下的最暗像素默认保持近黑（轮廓线、锻纹不被洗白）；
    给刀条表传 dark_v 时改成压成"余烬暗红"——烧透的钢没有纯黑区域，
    镡前那段原本发黑的暗带不再像一块脏斑（材质不匹配的观感就是这么来的）。"""
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    rnd = random.Random(seed)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 128:
                continue
            v = max(r, g, b) / 255.0
            if v <= keep_dark:
                if dark_v is None:
                    continue
                nv = dark_v * (0.55 + 0.45 * v / max(keep_dark, 1e-6))
                nh = (hue - 0.012) % 1.0
                nr, ng, nb = colorsys.hsv_to_rgb(nh, min(1.0, sat + 0.08), nv)
                px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
                continue
            nv = v_lo + (v_hi - v_lo) * v
            nh = (hue + rnd.uniform(-0.012, 0.012)) % 1.0
            ns = min(1.0, max(0.0, sat * (0.6 + 0.4 * v) + rnd.uniform(-0.08, 0.08)))
            nr, ng, nb = colorsys.hsv_to_rgb(nh, ns, nv)
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img


def edge_char(img, amount=0.55):
    """四边压暗：模拟铁锭边缘冷却发黑（只对亮像素起作用）。"""
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 128:
                continue
            d = min(x, y, w - 1 - x, h - 1 - y)
            if d <= 2:
                f = 1.0 - amount * (3 - d) / 3.0
                px[x, y] = (int(r * f), int(g * f), int(b * f), a)
    return img


def diagonal_streaks(img, hue, sat, value, width=2, step=5, seed=7):
    """叠加斜向条纹（融合钢的锻合纹）。"""
    px = img.load()
    w, h = img.size
    rnd = random.Random(seed)
    off = rnd.randint(0, step - 1)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 128:
                continue
            if (x + y + off) % step < width:
                nr, ng, nb = colorsys.hsv_to_rgb(hue, sat, value * (0.7 + 0.3 * rnd.random()))
                px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img


def speckles(img, hue, sat, vmin, vmax, count, size=2, seed=11):
    """随机斑块（黏土斑、冷却黑斑）：不规则小团，不是正圆点。"""
    px = img.load()
    w, h = img.size
    rnd = random.Random(seed)
    for _ in range(count):
        cx, cy = rnd.randrange(w), rnd.randrange(h)
        s = rnd.randint(1, size)
        v = rnd.uniform(vmin, vmax)
        col = colorsys.hsv_to_rgb((hue + rnd.uniform(-0.02, 0.02)) % 1.0, sat, v)
        col = (int(col[0] * 255), int(col[1] * 255), int(col[2] * 255))
        for dy in range(-s, s + 1):
            for dx in range(-s, s + 1):
                x, y = (cx + dx) % w, (cy + dy) % h
                # 抖动半径：同一格可能画也可能不画，斑块边缘就是毛的
                if dx * dx + dy * dy > s * s + rnd.uniform(-0.8, 1.2):
                    continue
                r, g, b, a = px[x, y]
                if a >= 128:
                    px[x, y] = (col[0], col[1], col[2], a)
    return img


def main():
    hot = Image.open(os.path.join(TEX_DIR, "sb_hot.png")).convert("RGBA")
    oodachi = Image.open(os.path.join(TEX_DIR, "sb_oodachi.png")).convert("RGBA")

    # ---- 三种烧红锭子（16x16）----
    iron = recolor(hot, hue=0.045, sat=0.85, v_lo=0.62, v_hi=1.0, seed=1)
    iron = edge_char(iron, 0.45)
    iron = speckles(iron, 0.02, 0.55, 0.10, 0.22, 8, size=1, seed=3)      # 焦黑冷斑
    iron.save(os.path.join(TEX_DIR, "sb_hot_iron.png"))

    steel = recolor(hot, hue=0.115, sat=0.6, v_lo=0.82, v_hi=1.0, seed=2)
    steel = speckles(steel, 0.13, 0.22, 1.0, 1.0, 5, size=1, seed=5)      # 白热点
    steel.save(os.path.join(TEX_DIR, "sb_hot_steel.png"))

    fused = recolor(hot, hue=0.97, sat=0.85, v_lo=0.6, v_hi=0.95, seed=3)
    fused = diagonal_streaks(fused, hue=0.68, sat=0.45, value=0.85, width=1, step=7, seed=9)
    fused = speckles(fused, 0.14, 0.15, 1.0, 1.0, 4, size=1, seed=13)     # 白火星
    fused.save(os.path.join(TEX_DIR, "sb_hot_fused.png"))

    # ---- r17 两种新钢的烧红锭子（16x16，同样从 sb_hot.png 派生）----
    # 本来想让低碳/高碳钢复用 sb_hot_steel 再乘色，但用户之前就反馈过
    # "共用一张表只靠乘色，颜色几乎一模一样看不出区别"，所以各给一张有
    # 自己纹理的表，HeldBladeModels 里的乘色一律回到 1,1,1。
    # 低碳钢：比钢锭再白一档。斜向暗纹会把它拉成米色（渲图看着像木板），
    # 所以只降饱和 + 多打几块白热斑，靠母表自身的顶亮底暗保住"烧红"的渐变。
    lowc = recolor(hot, hue=0.12, sat=0.30, v_lo=0.84, v_hi=1.0, seed=12)
    lowc = speckles(lowc, 0.13, 0.06, 1.0, 1.0, 9, size=1, seed=15)        # 白热片
    lowc.save(os.path.join(TEX_DIR, "sb_hot_lowcarbon.png"))

    highc = recolor(hot, hue=0.022, sat=0.95, v_lo=0.5, v_hi=0.92, seed=17)
    highc = speckles(highc, 0.0, 0.8, 0.22, 0.36, 9, size=1, seed=18)      # 暗红碳斑
    highc = speckles(highc, 0.06, 0.9, 0.95, 1.0, 4, size=1, seed=19)      # 几粒火星
    highc.save(os.path.join(TEX_DIR, "sb_hot_highcarbon.png"))

    # ---- 三种烧红刀条（320x128，整表重染；只有刀刃部件会用到这些表）----
    # dark_v：把母表里的近黑暗带也压成余烬暗红，整条刀身"通体烧透"，
    # 根部不再出现和橙红刀身对不上的脏黑带。
    blade = recolor(oodachi, hue=0.05, sat=0.9, v_lo=0.6, v_hi=1.0, keep_dark=0.16,
                    seed=4, dark_v=0.62)
    blade.save(os.path.join(TEX_DIR, "sb_hot_blade.png"))

    crude = recolor(oodachi, hue=0.005, sat=0.8, v_lo=0.3, v_hi=0.68, keep_dark=0.14,
                    seed=5, dark_v=0.40)
    crude = speckles(crude, 0.0, 0.5, 0.05, 0.13, 45, size=2, seed=17)    # 闷烧黑斑
    crude.save(os.path.join(TEX_DIR, "sb_hot_crude.png"))

    clay = recolor(oodachi, hue=0.038, sat=0.88, v_lo=0.55, v_hi=0.95, keep_dark=0.16,
                   seed=6, dark_v=0.55)
    clay = speckles(clay, 0.095, 0.35, 0.5, 0.75, 60, size=3, seed=23)    # 黏土黄斑块
    clay.save(os.path.join(TEX_DIR, "sb_hot_clay.png"))

    print("sheets ->", TEX_DIR)


if __name__ == "__main__":
    main()
