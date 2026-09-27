#!/usr/bin/env python3
"""派生三种铜刀镡贴图：形状与原刀镡逐像素一致，只把金属部分换成铜色。

- tsuba_pure_copper  ← tsuba_pure_gold 整体金→铜
- tsuba_wood_copper  ← tsuba_wood_gold 仅金圈→铜圈（木纹保持）
- tsuba_bamboo_copper← tsuba_bamboo_iron 仅灰圈→铜圈（竹纹保持）
"""
from PIL import Image
import os

BASE = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                    'assets', 'slashbladeresh_slashblad', 'textures', 'item')


def clamp(v):
    return max(0, min(255, int(round(v))))


def gold_to_copper(r, g, b):
    """金黄 → 铜橙：红稍降、绿明显降、蓝按绿分量抬起来。"""
    return clamp(r * 0.95), clamp(g * 0.72), clamp(g * 0.35 + 12)


def gray_to_copper(v):
    """铁灰亮度 → 铜色明暗梯度。"""
    return clamp(v * 1.35 + 30), clamp(v * 0.85 + 18), clamp(v * 0.55 + 12)


def remap(src, dst, predicate):
    im = Image.open(os.path.join(BASE, src)).convert('RGBA')
    out = Image.new('RGBA', im.size)
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = im.getpixel((x, y))
            if a > 0:
                r, g, b = predicate(r, g, b)
            out.putpixel((x, y), (r, g, b, a))
    out.save(os.path.join(BASE, dst))
    print('wrote', dst)


# 纯铜：整枚都是金属，金像素全转铜（近黑的轮廓保留）。
remap('tsuba_pure_gold.png', 'tsuba_pure_copper.png',
      lambda r, g, b: gold_to_copper(r, g, b) if r + g + b > 40 else (r, g, b))

# 铜木：只转亮金圈（绿分量≥115 的是金；木色绿分量≤80）。
remap('tsuba_wood_gold.png', 'tsuba_wood_copper.png',
      lambda r, g, b: gold_to_copper(r, g, b) if g >= 115 else (r, g, b))

# 铜竹：只转中性灰圈（最大最小差≤8 且非近黑）；竹的青黄纹保留。
remap('tsuba_bamboo_iron.png', 'tsuba_bamboo_copper.png',
      lambda r, g, b: gray_to_copper((r + g + b) / 3)
      if max(r, g, b) - min(r, g, b) <= 8 and r + g + b > 60 else (r, g, b))
