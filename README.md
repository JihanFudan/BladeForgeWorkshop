# 致谢 qwq
检视部分：感谢原作者 猫叩Mewkooo与全部原项目贡献者，本项目检视部分动作灵感与制作、检视实现代码、检视音效均来自于原项目 SlashBlade Inspect模组，进行二次制作。

# 锻刀工坊 自定义配方

适用于 Minecraft 1.21.1 + NeoForge。数据包就是放进存档、用于修改配方等规则的一组文件，无需修改模组 jar。

## 木偶与竹刀：手持刀条右键组装

木刀与竹刀不再走刀剑制作台。手持木刀条／竹刀条右键：

- 背包（含快捷栏与副手）里有对应刀镡（木条配木铁／木金／铜木刀镡，竹条配竹铁／竹金／铜竹刀镡；几种都有时按金＞铜＞铁取用）、刀柄、刀鞘各一件时，消耗材料并播放组装动画（刀镡上刀 → 刀柄安装 → 插入刀鞘，约 4 秒），动画结束成品刀入包。
- 缺任何一件时提示"背包内没有完整合成材料"，不消耗任何东西。
- 铁刀镡 攻+0 耐+5、金刀镡 攻+2 耐+1、铜刀镡 攻+1 耐+1；产物数值与旧制作台配方一致。
- 组装动画由 VMD 驱动：刀条／刀镡／刀柄／刀鞘四根骨骼的关键帧存在
  `src/main/resources/assets/slashbladeresh_slashblad/assembly/blade_assembly.vmd`，
  由 `BladeAssemblyClient` + `AssemblyMotion` 采样绘制。
- 四个部件用的是拔刀剑本体 jar 里 `blade.obj` 网格切出的**真 3D 模型**（不再是物品图标），
  网格与 UV 存在 `assembly/blade_parts.json`，贴图复用拔刀剑本体的材质表
  `textures/assembly/sb_wood.png`（木偶）与 `sb_bamboo.png`（竹光）：材质表横向平铺 5 份、
  UV 同步归一（原版有少量面沿表宽平铺采样，游戏内钳制采样会被"拉丝"），
  并补全了透明区（素木刀材质表的刀镡落区原本是透明的）。四部件共享同一坐标系，
  落座时天然对齐。舞台整体带 14°/12° 俯仰侧摆（同物品 GUI 变换思路），
  刀尖刀背有透视纵深。
   **r12 关键修复**：`AssemblyStageModel` 过去按"3 顶点/三角形"把网格喂进游戏渲染缓冲，
  而 `entityCutoutNoCull` 用的是 QUADS 顶点模式（GL 每 4 个顶点切一面、拆成三角形
  (0,1,2)+(2,3,0)）。顶点数不能被 4 整除时，相邻三角形会被错误连线，生成横跨刀身、
  甚至跨部件的细长"垃圾面"——这才是『模型被拉伸』的真凶（与贴图、倾角无关）。
  现改为每三角形补发一个与第 3 顶点重合的退化顶点凑满 4 个，退化面面积为零不可见，
  部件完整刚性平移、拉丝彻底消失。仿真验证见 `model-previews/quads_before.png`
  与 `quads_after.png`。
  中文创造栏标签与按键分类键在 `lang/zh_cn.json` 里为『锻刀工坊』
  （`itemGroup.slashbladeresh_slashblad` 与 `key.slashbladeresh_slashblad.category`）。
  切分脚本 `animation_tools/build_parts_model.py`，
  动画编排脚本 `animation_tools/assembly_vmd.py`，
  预览脚本 `animation_tools/preview_assembly.py`（流程网格）与
  `animation_tools/preview_textured.py`（逐像素贴图+倾角，模拟游戏内观感）。
- 想在 Blender 里改动画：导入免插件的模型+动画一体文件 `blender_preview/blade_assembly.glb`
  （物体名 blade_blank／tsuba／handle／sheath），改完导出同名 glb，再跑
  `animation_tools/export_assembly_vmd.py` 写回 VMD。详见 `blender_preview/如何改组装动画.txt`。
- 检视动画的 .vmd 仍在 `src/main/resources/assets/slashbladeresh_slashblad/combostate/`，
  Blender 工程在 `blender_preview/`。

## r14：刀条／刀鞘拿在手里的模型 = 拔刀剑本体的刀刃／刀鞘

手持（含掉落物、展示框）不再画平面图标，而是直接画拔刀剑本体 OBJ 网格分组：

| 物品 | 用的拔刀剑模型 | 材质表 |
| --- | --- | --- |
| 刀鞘 | 大太刀的刀鞘（`sheath` 分组，加饰长鞘） | `textures/assembly/sb_oodachi.png` |
| 成品／粗制／未完成／覆土／烫手／灼热刀条 | 大太刀的刀刃（`blade` 分组） | 同上，按工序乘色（灼热偏橙红、覆土偏米白） |
| 木刀条 | 木偶的刀刃（`blade` 分组） | `sb_wood.png` |
| 竹刀条 | 竹光的刀刃（`blade` 分组） | `sb_bamboo.png` |

- 本版本（重锋 2.0.5）没有单独叫"大太刀"的刀，同一套刀身网格靠材质表区分刀型；
  这里取的是**魔剑「阎魔刀」那套**（`slashblade:model/named/yamato.obj` + `yamato.png`，
  刀鞘是加饰长鞘）。想换成枯石大刀，把 `build_parts_model.py` 顶部的
  `ODACHI_OBJ` / `TEX_ODACHI` 改成 `named/dios/dios.obj` / `named/dios/koseki.png`
  重跑脚本即可，两份网格的分组名与坐标原点完全相同。
- 实现：`client/HeldBladeModels` 通过 `RegisterClientExtensionsEvent` 给这些物品挂
  `builtin/entity` 自定义渲染器；位置／朝向照抄原版 `item/generated`＋`item/handheld`
  的 `display` 数值（只把手持 scale 放大到拔刀剑那种分量），所以左右手、副手、
  掉落、展示框都走原版同一套变换，不会出现刀插进手臂。
- **背包格子仍用原来的平面图标**：真实比例的日本刀刀身只有 0.04 格宽，缩到 16 像素
  的格子里会糊成一条线。平面图标由伴生模型 `models/item/<name>_icon.json` 提供，
  渲染时直接借它烘好的几何，位置留白与别的物品一致。
- 手持部件网格与 `sb_oodachi.png` 由 `animation_tools/build_parts_model.py` 一并生成
  （存在同一份 `assembly/blade_parts.json` 的 `hold_*` 部件里，坐标系是物品模型
  坐标 0..1 立方、刀尖朝右上斜放）；观感自查脚本
  `animation_tools/preview_held_models.py`，输出 `model-previews/held_models_check.png`
  （背包位／第一人称手上／第三人称手上／展示框四种视角逐一体检）。

## 直接使用示例

1. 复制本工程 `examples/custom-recipes` 文件夹到存档的 `datapacks` 文件夹中。
2. 确认 `pack.mcmeta` 直接位于该文件夹内，不要多套一层目录。
3. 进入存档执行 `/reload`，或退出后重新进入。服务器由管理员操作。
4. 查询利刀白鞘的 JEI 配方，并在刀剑制作台制作。

示例覆盖利刀白鞘的内置配方，使用无铭刀「木偶」、两枚耀魂铁锭、一枚金锭、一件成品刀条和一件纯金属刀镡（纯铁／纯金／纯铜三选一）。示例包不会自动启用，也不打包进模组。

## 增加配方

文件路径：`data/你的命名空间/recipe/配方名.json`。

使用 Minecraft 1.21.1 的 `minecraft:crafting_shaped`（有序）或 `minecraft:crafting_shapeless`（无序）格式，也可使用已安装附属注册的拔刀剑配方格式。

- `result.id` 写真实物品注册名，普通成品的数量建议始终为 1。
- 同种材料需要两件，就在 `ingredients` 重复写两项。
- 多选材料用数组，例如示例中的纯铁／纯金／纯铜刀镡。
- 标签材料可写 `{"tag":"c:ingots/iron"}`。
- 有自定义数据的名刀，应使用其模组提供的配方格式或结果组件；不要只写基础拔刀剑物品名，否则无法代表特定名刀。
- 原配方最多九格，制作台额外补入的刀条与刀镡不占原配方网格。

虽然文件中的 `type` 仍写工作台配方，但产物只要是真正的拔刀剑，就会被刀剑制作台接管。无需自行注册新的配方类型。原工作台和原版自动合成器的正常配方查询不再允许产出它。

## 自动接管规则

- 检查产物具有拔刀剑状态，不限 `slashblade` 命名空间，因此其他附属继承拔刀剑物品的刀也能识别。
- 接管有固定拔刀剑产物的工作台配方和锻造台配方，保留其原材料、物品组件条件、原始 `matches` 检查、`assemble` 产物及容器返还物。
- 材料中没有独立成品刀条要求时，追加一件成品刀条。
- 材料中没有独立的纯金属刀镡要求时，追加一件纯铁／纯金／纯铜三选一刀镡。宽泛标签不能替代这项要求。
- 玩家逐件放材料，不需要复刻工作台格子位置；实际制作前会恢复原有序网格并检查原配方。
- 杀敌、耀魂、精炼等条件仍由原配方检查，不能仅凭相同物品名绕过。
- 产物保留原配方生成的外观、招式、组件与附魔，并至少保留原刀的养成计数；攻击继承后叠加本次材料修正。
- 配方重载后制作台自动读取新配方。若 JEI 未刷新，可退出并重进存档。

兼容范围：标准工作台与锻造台配方，包括基于这些接口的附属配方。完全不公开材料或没有固定预览产物的动态配方、其他模组自建机器、自行绕过配方管理器的合成逻辑，需要针对该附属额外适配；不能凭空推导它们的合成规则。掉落、创造模式和指令获取不受影响。

## 覆盖内置配方

在下列路径放同 ID 的正常拔刀剑配方，即可取代对应的内置清单：

| 配方 ID | 对应成品 |
| --- | --- |
| `slashbladeresh_slashblad:wood` | 木刀 |
| `slashbladeresh_slashblad:bamboo` | 竹刀 |
| `slashbladeresh_slashblad:silver_bamboo` | 银纸竹光 |
| `slashbladeresh_slashblad:white_sheath` | 利刀白鞘 |
| `slashbladeresh_slashblad:mumei` | 无名 |
| `slashbladeresh_slashblad:ruby` | 红玉 |

例如利刀白鞘使用 `data/slashbladeresh_slashblad/recipe/white_sheath.json`，不要使用 `ruby_definition` 这个内部产物定义 ID。

覆盖其他附属配方时使用该配方原本的命名空间与路径。添加一条不同 ID 的配方只会增加路线，不会删除原路线。

## 精炼递减规则

- 耀魂锻打第 n 次的基础攻击增量：`5 × ln(1 + (n+1)/20) − 5 × ln(1 + n/20)`，其中 n 是锻打前的原生精炼次数。
- 从零开始累计 10、100、1000 次耀魂锻打，基础攻击分别约增加 2.03、8.96、19.66，不再每次固定增加 1。
- 本体战斗中原来的线性精炼伤害项 x，改为 `10 × ln(1 + max(0,x)/10)`；段位基础值、S 级门槛、玩家等级限制和其他伤害因素保留。
- 只压缩增益，不减少精炼计数，不清空旧刀的杀敌、耀魂或已保存的基础攻击。

## r17：碳粉与两种碳钢（低碳钢／高碳钢）

金属线从「铁＋钢两色配比」改成「碳粉配出三种钢，五枚同种融一刀」：

| 环节 | 工位 | 操作 | 产物 |
| --- | --- | --- | --- |
| 研磨 | 锻造铁砧 | 煤炭／木炭右键放上，再用锻造锤右键 | 碳粉 ×4 |
| 配低碳钢 | 刀剑制作台 | 钢锭 ×1 ＋ 黏土球 ×1 ＋ 碳粉 ×1，锻造锤右键 | 低碳钢 |
| 配高碳钢 | 刀剑制作台 | 钢锭 ×1 ＋ 黏土球 ×1 ＋ 碳粉 ×2，锻造锤右键 | 高碳钢 |
| 烧红 | 烧铁炉 | 钢锭／低碳钢／高碳钢各 5 枚一批，约 8 秒 | 对应灼热钢 |
| 第一锤 | 锻造铁砧 | 同一种灼热钢凑满 5 枚，锻造锤右键 | 融合钢（带钢材记录） |

- 钢材记录取代旧的铁钢配比，一路从融合钢传到成品刀条：
  钢 攻＋3／耐＋3，低碳钢 攻＋2／耐＋4，高碳钢 攻＋4／耐＋2。
  一炉只融同一种钢，混放会提示先取出。
- 铁锭退居炼钢原料：单枚灼热铁锭在铁砧炼成钢锭，不再参与融合。
- 灼热低碳钢／灼热高碳钢与其余灼热锭材共用同一套钳子夹取网格
  （`hold_ingot` / `hold_ingot_grip` / `hold_tongs_ingot`），
  材质表各一张：`sb_hot_lowcarbon.png`（近白热）、`sb_hot_highcarbon.png`（深橙红带碳斑），
  由 `animation_tools/gen_hot_sheets.py` 从母表 `sb_hot.png` 派生，
  不再靠乘色复用钢锭表——之前那样三种红锭子肉眼分不出来。
- 五件新物品的背包图标由 `animation_tools/gen_carbon_items.py` 从现有图标按调色板派生，
  轮廓与钢锭一致；碳粉是手绘的一小堆粉末。渲图自查见
  `model-previews/carbon_icons.png`、`carbon_sheets.png`、`held_ingots_check.png`。
- 手册同步：刀镡路线的「一根竹子」改成「一块竹板」（实际收的就是竹板），
  新增「先炼钢，再研磨碳粉」「锻打两种碳钢」两页，
  「融合与锻打」「钢材种类与属性」按新规则重写；JEI 物品信息页同步。
