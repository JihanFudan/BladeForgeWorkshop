# -*- coding: utf-8 -*-
"""
gen_steel_set.py —— 1.0.4-r18：整套钢材贴图重画（背包图标 + 3D 材质表）。

上一版的问题（用户反馈"材质有一些错乱颜色"）：
  * 冷钢锭是从旧图重染来的，右端残留一坨橙褐色"锈斑"、还夹着紫色像素；
  * 融合钢图标上有几颗紫红杂点，3D 表 sb_hot_fused.png 更是红紫底配蓝斜纹；
  * 低碳钢烧红后偏米黄，看着像奶酪不像热金属。
这一版不再从旧图派生，全部按同一套锭形重画，每种金属只用自己的调色板：

  冷料（银灰 → 暖橙红 → 深青灰 → 折叠纹）
    steel_ingot.png        钢锭      冷银灰，刃口一条亮高光
    low_carbon_steel.png   低碳钢锭  橙红金属色（用户指定）
    high_carbon_steel.png  高碳钢锭  深青灰 + 炭黑颗粒 + 一道回火蓝纹
    fused_steel.png        融合钢    冷灰底 + 暖橙/青灰相间的折叠锻纹

  热料（同一种锭形，靠"芯亮边暗"的温度场表现烧红）
    heated_iron.png         灼热铁锭      橙黄，四边焦黑
    heated_steel.png        灼热钢锭      亮黄白
    heated_low_carbon.png   烧红低碳钢    橙红（不再偏黄）
    heated_high_carbon.png  烧红高碳钢    深红 + 暗红碳斑
    heated_fused_steel.png  烧热融合钢    橙红 + 金黄锻合纹

  3D 材质表（铺满 16x16，贴在 hold_ingot / hold_ingot_flat / hold_ingot_grip 上）
    sb_hot_iron / sb_hot_steel / sb_hot_lowcarbon / sb_hot_highcarbon / sb_hot_fused

用法: python3 animation_tools/gen_steel_set.py
"""
import math
import os
import random

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "item")
SHEET_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "assembly")

# 锭形：一行一行给出 [左, 右] 闭区间（16x16）。
# 压扁的斜视长条（宽高比约 2.4:1）：y=5 是顶面后沿，y=6 是顶面前沿（最亮），
# y=7~8 是正面，y=9 是底沿。上一版画成 13x9 的圆八边形，渲出来像只碗，
# 这一版照"从上往下看一根铁锭"的透视重画。
BAR = {
    5: (4, 12),
    6: (2, 13),
    7: (2, 13),
    8: (2, 13),
    9: (3, 12),
}
TOP, BOTTOM = 5, 9
TOPFACE = (5, 6)        # 顶面（受光）
FRONTFACE = (7, 8)      # 正面（本体）


def rgb(h):
    """#rrggbb -> (r,g,b)"""
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


class Palette:
    """一种金属的六档色：轮廓 / 暗面 / 本体 / 亮面 / 高光 / 点缀。"""

    def __init__(self, edge, dark, body, light, hi, accent, accent2=None):
        self.edge, self.dark, self.body = rgb(edge), rgb(dark), rgb(body)
        self.light, self.hi, self.accent = rgb(light), rgb(hi), rgb(accent)
        self.accent2 = rgb(accent2) if accent2 else self.accent


COLD = {
    # 钢：冷银灰，蓝味一点点，别发紫
    "steel": Palette("#191f28", "#5a6472", "#98a5b3", "#c6d1dc", "#f2f7fc", "#7c8998"),
    # 低碳钢：用户指定的橙红金属色（上一版偏砖棕，这里把色相推到橙红）
    "lowcarbon": Palette("#3a1207", "#8a3418", "#c8552e", "#e8814f", "#ffc59a", "#a8401e"),
    # 高碳钢：深青灰 + 炭黑颗粒 + 一道回火蓝纹
    "highcarbon": Palette("#0f151d", "#3e4855", "#6b7889", "#9aaabd", "#d8e6f5", "#1b222c", "#8fb6d8"),
    # 融合钢：冷灰底 + 暖橙折叠锻纹（混熔的身份标记）
    "fused": Palette("#171d25", "#4c5663", "#8d99a8", "#c2cdd9", "#f1f7fd", "#d98a50", "#6f8ba6"),
}
HOT = {
    "iron": Palette("#2a1206", "#7a3312", "#c25c1e", "#f0913c", "#ffd08a", "#3d1a08"),
    "steel": Palette("#4a2408", "#a8561c", "#e8912e", "#ffc15c", "#ffeec0", "#7a3c10"),
    "lowcarbon": Palette("#4a1405", "#a82c0e", "#e2551c", "#ff9a52", "#ffd0a8", "#7a1e08"),
    "highcarbon": Palette("#38080a", "#7d1410", "#c22a18", "#f05a2a", "#ffb070", "#4a0e0a"),
    # 融合钢：橙红底 + 金黄锻合纹（accent2），不再有蓝紫斜纹
    "fused": Palette("#40120a", "#96361a", "#d8622a", "#ffa55e", "#ffe8c0", "#5c2410", "#ffd878"),
}


def band_color(pal, y, x, hot):
    """按所在行取基础色：顶面受光最亮，正面是本体，底沿压暗。
    热料整体走"上白热 → 下冷却发黑"的温度梯度，边上再焦一档。"""
    if hot:
        if y == 5:
            return mix(pal.light, pal.hi, 0.55)
        if y == 6:
            return pal.hi
        if y == 7:
            return pal.light
        if y == 8:
            return pal.body
        return mix(pal.dark, pal.edge, 0.25)
    if y == 5:
        return pal.light
    if y == 6:
        return mix(pal.light, pal.hi, 0.55)
    if y == 7:
        return pal.body
    if y == 8:
        return mix(pal.body, pal.dark, 0.45)
    return mix(pal.dark, pal.edge, 0.35)


def draw_ingot(pal, hot=False, damascus=False, flecks=0, temper=False, seed=5):
    """画一根锭子：铺色 → 锻纹 → 高光/暗斑 → 碳斑 → 描边 → 火星。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed)
    for y, (x0, x1) in BAR.items():
        for x in range(x0, x1 + 1):
            c = band_color(pal, y, x, hot)
            # 两端压暗：斜视的端面在暗侧，体积感就出来了
            if x == x0 or x == x1:
                c = mix(c, pal.edge, 0.45 if hot else 0.35)
            elif x == x0 + 1 or x == x1 - 1:
                c = mix(c, pal.edge, 0.15)
            px[x, y] = (*c, 255)
    # 折叠锻纹（融合钢）：横向波浪带，暖橙与青灰交替。这是"多种钢叠打"的身份标记，
    # 所以带子要够宽够清楚，上一版几乎看不出来，和普通钢锭没差。
    if damascus:
        for y in (6, 7, 8):
            x0, x1 = BAR[y]
            for x in range(x0 + 1, x1):
                wave = (y * 2 + x + (1 if y == 7 else 0)) % 5
                if wave < 2:
                    px[x, y] = (*mix(px[x, y][:3], pal.accent, 0.80), 255)
                elif wave == 2:
                    px[x, y] = (*mix(px[x, y][:3], pal.hi, 0.45), 255)
                elif wave == 3:
                    px[x, y] = (*mix(px[x, y][:3], pal.accent2, 0.55), 255)
    # 顶面高光：断成两段，比一整条白线更像金属反光
    for x in (5, 6, 7, 9, 10):
        px[x, 6] = pal.hi
    # 热料：正面留几块冷却暗斑（出炉的锭子不会匀色），冷料：一道磨过的亮面
    if hot and not damascus:
        for _ in range(3):
            x = rnd.randint(3, 12)
            px[x, 8] = (*mix(px[x, 8][:3], pal.edge, 0.40), 255)
    else:
        for x in range(4, 12):
            px[x, 5] = mix(pal.light, pal.hi, 0.30)
    # 炭黑颗粒（高碳钢）
    for _ in range(flecks):
        x = rnd.randint(3, 12)
        y = rnd.choice(FRONTFACE)
        px[x, y] = (*pal.accent, 255)
        if rnd.random() < 0.5 and px[x + 1, y][3] >= 128:
            px[x + 1, y] = (*mix(px[x + 1, y][:3], pal.accent, 0.7), 255)
    # 回火蓝纹（高碳钢）：正面斜走一道亮蓝，像淬火留下的色带
    if temper:
        for i, x in enumerate(range(3, 12)):
            y = 7 + (i // 4)
            if px[x, y][3] >= 128:
                px[x, y] = (*mix(px[x, y][:3], pal.accent2, 0.5), 255)
    # 描边：贴着空格的像素压暗，剪影才立得住
    for y in range(16):
        for x in range(16):
            if px[x, y][3] < 128:
                continue
            if any(px[x + dx, y + dy][3] < 128
                   for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))
                   if 0 <= x + dx < 16 and 0 <= y + dy < 16):
                px[x, y] = (*mix(px[x, y][:3], pal.edge, 0.6), 255)
    # 火星：热料只飘一粒余烬，紧贴锭子右上角。两粒白点会被看成"芝麻"。
    if hot:
        px[13, 4] = (*mix(pal.hi, pal.body, 0.35), 255)
    return img


def sheet(pal, damascus=False, char=False, flecks=0, sparks=6, seed=3):
    """3D 材质表：铺满 16x16，顶亮底暗 + 纵向锻纹，颜色与图标同一套。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed)
    for y in range(16):
        t = y / 15.0
        base = mix(pal.light, pal.body, min(1.0, t * 1.5)) if t < 0.62 else mix(pal.body, pal.dark, (t - 0.62) / 0.38)
        for x in range(16):
            c = base
            # 纵向锻纹：每隔几列压一点亮暗，贴到锭子上就是锻打留下的纹路
            g = (x + y // 3) % 5
            if g == 0:
                c = mix(c, pal.accent2, 0.30)
            elif g == 2:
                c = mix(c, pal.hi, 0.16)
            c = mix(c, pal.edge if char else pal.dark, rnd.uniform(0.0, 0.10))
            px[x, y] = (*c, 255)
    if damascus:
        # 锻合纹：宽间隔的斜带，金黄一暗红交替。上一版每 9 格一条太密，
        # 贴在锭子上看着像华夫饼，这里放宽到 1/6 并降低对比。
        for y in range(16):
            for x in range(16):
                k = (x + y * 2) % 13
                if k == 0:
                    px[x, y] = (*mix(px[x, y][:3], pal.accent2, 0.62), 255)
                elif k == 1:
                    px[x, y] = (*mix(px[x, y][:3], pal.hi, 0.30), 255)
                elif k == 7:
                    px[x, y] = (*mix(px[x, y][:3], pal.edge, 0.30), 255)
    if char:
        # 铁锭最外圈焦黑：出炉的铁总带一层氧化皮（只压最外一圈，
        # 压两圈会让整张贴图边缘发暗，贴到锭子上像蒙了灰）
        for y in range(16):
            for x in range(16):
                if min(x, y, 15 - x, 15 - y) == 0:
                    px[x, y] = (*mix(px[x, y][:3], pal.edge, 0.42), 255)
    for _ in range(flecks):
        # 碳斑：单格 + 偶尔邻格，压到 0.55 就够。上一版画成 2 格半径的实心团，
        # 贴在锭子上像几个窟窿，而不是钢里的碳粒。
        x, y = rnd.randrange(16), rnd.randrange(16)
        px[x, y] = (*mix(px[x, y][:3], pal.accent, 0.55), 255)
        if rnd.random() < 0.55:
            xx, yy = (x + rnd.choice((-1, 1))) % 16, (y + rnd.choice((0, 1))) % 16
            px[xx, yy] = (*mix(px[xx, yy][:3], pal.accent, 0.42), 255)
    for _ in range(sparks):
        # 火星：不是纯白点，掺一点本体色才像金属上的反光/余烬
        x, y = rnd.randrange(16), rnd.randrange(16)
        px[x, y] = (*mix(pal.hi, pal.light, 0.4), 255)
    return img


def save(img, folder, name):
    out = os.path.join(folder, name + ".png")
    img.save(out)
    print("  ->", os.path.relpath(out, ROOT))


def main():
    # ---- 冷料图标 ----
    save(draw_ingot(COLD["steel"], seed=1), ITEM_DIR, "steel_ingot")
    save(draw_ingot(COLD["lowcarbon"], seed=2), ITEM_DIR, "low_carbon_steel")
    save(draw_ingot(COLD["highcarbon"], flecks=7, temper=True, seed=3), ITEM_DIR, "high_carbon_steel")
    save(draw_ingot(COLD["fused"], damascus=True, seed=4), ITEM_DIR, "fused_steel")
    # ---- 热料图标 ----
    save(draw_ingot(HOT["iron"], hot=True, seed=5), ITEM_DIR, "heated_iron")
    save(draw_ingot(HOT["steel"], hot=True, seed=6), ITEM_DIR, "heated_steel")
    save(draw_ingot(HOT["lowcarbon"], hot=True, seed=7), ITEM_DIR, "heated_low_carbon")
    save(draw_ingot(HOT["highcarbon"], hot=True, flecks=6, seed=8), ITEM_DIR, "heated_high_carbon")
    save(draw_ingot(HOT["fused"], hot=True, damascus=True, seed=9), ITEM_DIR, "heated_fused_steel")
    # ---- 3D 材质表 ----
    save(sheet(HOT["iron"], char=True, flecks=3, seed=11), SHEET_DIR, "sb_hot_iron")
    save(sheet(HOT["steel"], seed=12), SHEET_DIR, "sb_hot_steel")
    save(sheet(HOT["lowcarbon"], seed=13), SHEET_DIR, "sb_hot_lowcarbon")
    save(sheet(HOT["highcarbon"], flecks=9, seed=14), SHEET_DIR, "sb_hot_highcarbon")
    save(sheet(HOT["fused"], damascus=True, sparks=3, seed=15), SHEET_DIR, "sb_hot_fused")
    print("OK: 9 张图标 + 5 张材质表")


if __name__ == "__main__":
    main()
