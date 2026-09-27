# -*- coding: utf-8 -*-
"""
gen_iron_sheet.py —— 重画钳子材质表 sb_iron.png（16x16）。

布局与 build_parts_model.py 里的 UV 带一一对应：
  纵向三段：行 0-4 亮面（顶）、行 5-10 中调（侧）、行 11-15 暗面（底）；
  横向两半：左半 x 0-7 = 冷灰铁（钳臂/铆钉/钳柄），
            右半 x 8-15 = 热渍暖铁（钳口——贴着烧红工件烤出火色）。
逐像素 ±10 抖动 + 少量深色麻点，避免整块塑料感。

用法: python3 animation_tools/gen_iron_sheet.py
"""
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", NS,
                   "textures", "assembly", "sb_iron.png")

COOL = ((138, 146, 158), (96, 102, 112), (52, 55, 63))     # 亮 / 中 / 暗
WARM = ((118, 96, 80), (84, 66, 56), (50, 40, 36))        # 火烤发暗的暖铁，不是黄铜


def band(y):
    return 0 if y <= 4 else (1 if y <= 10 else 2)


def main():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rnd = random.Random(42)
    for y in range(16):
        b = band(y)
        for x in range(16):
            base = WARM[b] if x >= 8 else COOL[b]
            j = rnd.randint(-10, 10)
            r, g, bl = [max(0, min(255, c + j + rnd.randint(-4, 4))) for c in base]
            # 热渍那一半再压一点暖色偏差，做出火烤的不均匀感
            if x >= 8:
                r = max(0, min(255, r + rnd.randint(-6, 10)))
                bl = max(0, bl - rnd.randint(0, 8))
            px[x, y] = (r, g, bl, 255)
    for _ in range(14):  # 麻点
        x, y = rnd.randrange(16), rnd.randrange(16)
        r, g, b, a = px[x, y]
        px[x, y] = (r // 2, g // 2, b // 2, 255)
    img.save(OUT)
    print("IRON ->", OUT)


if __name__ == "__main__":
    main()
