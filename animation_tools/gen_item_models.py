# -*- coding: utf-8 -*-
"""
gen_item_models.py —— 批量生成"手持 3D 网格 + 背包平面图标"两套物品模型 JSON。

手持部件（HeldBladeModels）用 builtin/entity 挂自定义渲染器，位置/朝向全交给
display 段。这一轮的 display 数值配合 build_parts_model.py 里新的网格方位角
（刀尖从"朝右上 45°"抬到 102°，握在手里刀尖向前倾）：

  * 第一人称 rz 保持 25° —— 屏幕上的斜度只由 rz 决定，所以手持的斜看感和以前一致，
    变的是"刀尖往前压"这个深度方向；
  * 第三人称 rz 55° -> 0° —— 补掉网格多转的那 55°，观感不变；
  * 掉落(ground)/展示框(fixed) 各自补 -57°/+57° —— 摆在那儿还是原来的 45° 斜放。

用法: python3 animation_tools/gen_item_models.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NS = "slashbladeresh_slashblad"
MODEL_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "models", "item")

# 手持 3D 网格物品的 display 段（translation 单位 = 1/16 格）
DISPLAY_BLADE = {
    "ground": {"rotation": [0, 0, -57], "translation": [0, 2, 0], "scale": [0.9, 0.9, 0.9]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 4.0, 0.5], "scale": [1.6, 1.6, 1.6]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 4.0, 0.5], "scale": [1.6, 1.6, 1.6]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [2.2, 2.2, 2.2]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [2.2, 2.2, 2.2]},
    "fixed": {"rotation": [0, 180, 57], "translation": [0, 0, 0], "scale": [1.5, 1.5, 1.5]},
}

# 锭子比刀条小得多，手持放大一点才看得见（网格本身只占立方里约 0.2 格）
DISPLAY_INGOT = {
    "ground": {"rotation": [0, 0, -57], "translation": [0, 2, 0], "scale": [1.4, 1.4, 1.4]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1.6, 1.6, 1.6]},
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 3.0, 0.5], "scale": [1.9, 1.9, 1.9]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 3.0, 0.5], "scale": [1.9, 1.9, 1.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [2.6, 2.6, 2.6]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [2.6, 2.6, 2.6]},
    "fixed": {"rotation": [0, 180, 57], "translation": [0, 0, 0], "scale": [2.2, 2.2, 2.2]},
}

# 手持走 3D 网格的物品（刀条家族 + 刀鞘）
BLADE_ITEMS = [
    "blade_sheath", "quenched_blade", "crude_blade", "unfinished_blade", "clay_blade",
    "hot_blade", "heated_crude_blade", "heated_clay_blade",
    "wooden_blade_blank", "bamboo_blade_blank",
]
# 烧红锭子：没带钳子时手里就是一块烧红的铁，带钳子时是钳子夹着它
# （r17 加了灼热低碳钢 / 灼热高碳钢，走同一套锭子网格与 display）
INGOT_ITEMS = ["heated_iron", "heated_steel", "heated_low_carbon", "heated_high_carbon",
               "heated_fused_steel"]
# 冷料：背包格子里的平面图标，没有手持 3D 网格
COLD_ITEMS = ["carbon_powder", "low_carbon_steel", "high_carbon_steel"]


def write(name, doc):
    path = os.path.join(MODEL_DIR, name + ".json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
    print("  ->", os.path.relpath(path, ROOT))


def main():
    os.makedirs(MODEL_DIR, exist_ok=True)
    for name in BLADE_ITEMS + INGOT_ITEMS:
        display = DISPLAY_INGOT if name in INGOT_ITEMS else DISPLAY_BLADE
        write(name, {"parent": "builtin/entity", "gui_light": "front", "display": display})
        # 背包格子里的平面图标：直接借原版烘好几何的 item/generated
        write(name + "_icon", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": NS + ":item/" + name},
        })
    # 冷料只有平面图标，走普通的 item/generated
    for name in COLD_ITEMS:
        write(name, {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": NS + ":item/" + name},
        })
    print("OK: %d 件手持物品 + %d 份平面图标 + %d 件冷料图标"
          % (len(BLADE_ITEMS) + len(INGOT_ITEMS), len(BLADE_ITEMS) + len(INGOT_ITEMS), len(COLD_ITEMS)))


if __name__ == "__main__":
    main()
