# -*- coding: utf-8 -*-
"""preview_grip_detail.py —— 把"钳子夹工件"两层网格放大渲染，专门查穿模。

用法: python3 animation_tools/preview_grip_detail.py
输出: model-previews/grip_detail.png
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

import preview_held_models as P

SIZE = 420


def main():
    parts = P.load_parts()
    sheets = {n: Image.open(os.path.join(P.SHEET_DIR, n)).convert("RGBA")
              for n in ("sb_oodachi.png", "sb_hot_blade.png", "sb_iron.png",
                        "sb_hot.png", "sb_hot_iron.png", "sb_wood.png")}

    def layers(work_part, work_sheet, tongs_part, view, disp):
        ls = []
        for part, sheet in ((work_part, work_sheet), (tongs_part, "sb_iron.png")):
            tris = P.apply_display(parts[part], disp)
            if view in ("fp", "tp"):
                tris = P.apply_hand(tris, view == "fp")
            ls.append((tris, sheets[sheet]))
        return ls

    combos = [
        ("夹刀条 第一人称", layers("hold_blade_grip", "sb_hot_blade.png", "hold_tongs_blade", "fp", "firstperson")),
        ("夹刀条 第三人称", layers("hold_blade_grip", "sb_hot_blade.png", "hold_tongs_blade", "tp", "thirdperson")),
        ("夹刀条 正前视", layers("hold_blade_grip", "sb_hot_blade.png", "hold_tongs_blade", "gui", "gui")),
        ("夹锭子 第一人称", layers("hold_ingot_grip", "sb_hot_iron.png", "hold_tongs_ingot", "fp", "firstperson")),
        ("夹锭子 正前视", layers("hold_ingot_grip", "sb_hot_iron.png", "hold_tongs_ingot", "gui", "gui")),
        ("裸刀条 第一人称", [(P.apply_hand(P.apply_display(parts["hold_blade_metal"], "firstperson"), True), sheets["sb_hot_blade.png"])], ),
        ("刀鞘 第一人称", [(P.apply_hand(P.apply_display(parts["hold_sheath"], "firstperson"), True), sheets["sb_wood.png"])], ),
        ("刀鞘 正前视", [(parts["hold_sheath"], sheets["sb_wood.png"])], ),
    ]
    cols = 4
    rows = (len(combos) + cols - 1) // cols
    canvas = Image.new("RGBA", (cols * (SIZE + 10), rows * (SIZE + 26)), (16, 16, 20, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(canvas)
    for i, (label, ls) in enumerate(combos):
        x = (i % cols) * (SIZE + 10)
        y = (i // cols) * (SIZE + 26)
        d.text((x + 4, y + 4), label, fill=(235, 235, 235, 255))
        view = "gui" if "正前视" in label else ("fp" if "第一人称" in label or "裸" in label or "刀鞘" in label else "tp")
        canvas.alpha_composite(P.render(ls, size=SIZE, view=view), (x, y + 22))
    out = os.path.join(ROOT, "model-previews", "grip_detail.png")
    canvas.convert("RGB").save(out)
    print("OK ->", out)


if __name__ == "__main__":
    main()
