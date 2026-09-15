package cn.blockforge.generated.slashbladereshslashblad.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/**
 * 《锻刀工坊游玩手册》：一本预写好的成书。
 * 玩家每次进入存档时自动发放一本（背包已有则不重复给）。
 */
public class GuideBookItem extends WrittenBookItem {
    public static final String TITLE = "锻刀工坊游玩手册";
    public static final String AUTHOR = "jihanfudan";

    /** 成书页面一行最多约 12 个汉字，保证书页不裁字；\n 在书页里正常换行。 */
    private static final List<String> PAGES = List.of(
            "锻刀工坊游玩手册\n\n本手册按实际操作顺序，\n讲解工具、工位、刀条、\n刀镡和拔刀剑的制作方法。\n\n后面的页面包含木刀、\n竹刀、银纸竹光、利刀白鞘、\n无名、红玉的完整材料清单，\n以及升级、精炼和取回方法。",
            "制作入门\n\n本模组通过专用工位\n完成刀条、刀镡和刀剑\n的制作与强化。\n\nJEI 是查看配方的模组。\n安装后可查看完整的\n制作方法与材料数量。\n\n本手册按实际操作顺序\n讲解制刀与强化流程。",
            "如何查询配方\n\n打开背包，在 JEI 中\n搜索物品，鼠标指向它\n按 R 查看制作方法，\n按 U 查看用途。\n\n名刀选择刀剑制作台\n分类；材料角标是数量，\n刀镡轮换显示为二选一。\n工位加工材料还可查看\n其信息页中的操作说明。",
            "准备工具与工位\n\n在工作台合成切割刀、\n雕刻凿、锻造锤和钳子。\n切割刀与凿用于木料，\n锤用于锻造，钳用于出炉。\n\n再合成并放置四种工位：\n刀镡制作台、烧铁炉、\n锻造铁砧、刀剑制作台。\n工具和工位的材料摆法\n均可在 JEI 中查询。",
            "先认识操作方式\n\n拿材料右键工位放入，\n换工具右键推进加工。\n操作提示显示在快捷栏\n上方，请按提示继续。\n\n双手加工时，主手拿\n工具，副手格放材料。\n默认按 F 可交换双手。\n不要蹲下，朝空中右键，\n加工产物会放入背包。",
            "制作刀镡与配件\n\n在空刀镡制作台上：\n① 放一块木板或一根竹子\n② 用切割刀右键一次\n③ 用雕刻凿右键一次\n④ 放一枚铁锭或金锭\n⑤ 再用雕刻凿右键一次\n\n空手右键，一并取出\n刀镡、刀柄和刀鞘。\n刀条需按下一页另做。",
            "制作木刀条或竹刀条\n\n主手拿切割刀，副手放\n木板，朝空中右键，\n得到木质刀条雏形。\n\n再把雏形放入副手，\n主手换雕刻凿右键，\n得到木刀条。\n\n使用竹板走同样步骤，\n得到竹质雏形和竹刀条。",
            "组装木刀与竹刀\n\n在空刀剑制作台逐件放：\n木刀条或竹刀条×１\n成品刀条×１\n木刀用木铁或木金刀镡\n竹刀用竹铁或竹金刀镡\n刀柄×１、刀鞘×１\n\n用锻造锤右键完成。\n木刀条出木刀，竹出竹刀。\n空手右键取回全部材料。\n金属工序见后续章节。",
            "制作纯金属刀镡\n\n手持至少四枚同种铁锭\n或金锭，右键空的\n刀镡制作台，消耗四枚。\n\n用雕刻凿右键加工，\n再空手右键取出一枚\n纯铁刀镡或纯金刀镡。\n\n这条路线只产出刀镡。\n名刀配方所需刀镡见后文。",
            "烧铁炉的使用\n\n手持铁锭或钢锭右键炉子，\n一次放入同种锭材最多\n五枚，约八秒烧红。\n炉子无需另添燃料。\n\n手持钳子右键取出，\n再换灼热锭材右键铁砧。\n\n炉内这一批取完后，\n才能放入下一批材料。",
            "先炼钢，再配比\n\n需要钢锭时，只放一枚\n灼热铁锭到空锻造铁砧，\n用锻造锤右键得到钢锭，\n再入炉烧成灼热钢锭。\n\n锻刀条时，用灼热铁锭\n和灼热钢锭凑足五枚。\n每次右键放一枚，\n铁与钢的数量可自行搭配。",
            "融合与锻打\n\n第一锤：五枚灼热锭材\n放齐后，用锻造锤右键，\n得到融合钢。\n\n第二锤：融合钢入炉，\n烧红后用钳子取出，\n放上铁砧锤成粗制刀条。\n\n第三锤：粗制刀条同样\n烧红、取出、上砧锤击，\n得到未完成的刀条。",
            "覆土与最后一锤\n\n主手拿未完成的刀条，\n副手拿一个黏土球，\n右键空烧铁炉。\n\n刀条消耗一团黏土覆土，\n并直接在炉内加热。\n约八秒后用钳子夹取。\n\n放上空锻造铁砧，\n用锻造锤右键，\n得到烫手的刀条。",
            "淬火得到成品刀条\n\n手持烫手的刀条，右键\n装有水的炼药锅，\n立即得到成品刀条。\n\n也可放在背包里，\n从最后一锤起等约三十秒，\n自然冷却成成品刀条。\n\n两种方法均保留铁钢配比，\n成品用于后面的名刀升级。",
            "铁钢配比与属性\n\n共五枚锭材，钢／铁：\n０／５：攻－３，耐＋５\n１／４：攻－２，耐＋５\n２／３：攻＋１，耐＋５\n３／２：攻＋２，耐＋３\n４／１：攻＋４，耐不变\n５／０：攻＋５，耐－５\n\n这是名刀制作时的\n攻击与耐久上限修正。",
            "刀镡使用规则\n\n木偶：木铁或木金刀镡\n竹光：竹铁或竹金刀镡\n其他所有拔刀剑：\n纯铁或纯金刀镡\n\n纯铁：耐久＋５\n纯金：攻击＋２，耐久＋１\n木质、竹质刀镡不能用于\n木偶和竹光以外的拔刀剑。\n\n攻击与耐久上限修正\n由成品刀条和刀镡叠加。",
            "名刀升级的操作\n\n先放前置刀，再按配方\n逐项右键放入材料。\n前置刀就是这次升级\n要使用的那把刀。\n\n每次右键只放入一件，\n数量不足时继续补放。\n齐全后用锻造锤右键，\n消耗刀与材料得到新刀，\n锤子保留，成刀进入背包。",
            "银纸竹光\n\n先放：竹刀×１\n再放：\n成品刀条×１\n纯铁或纯金刀镡×１\n纸×１\n黑色染料×１\n\n材料齐全后，\n用锻造锤右键完成。",
            "利刀白鞘\n\n先放：木刀×１\n再放：\n耀魂铁锭×２\n金锭×１\n成品刀条×１\n纯铁或纯金刀镡×１\n\n材料齐全后，\n用锻造锤右键完成。",
            "无名\n\n先放：利刀白鞘×１\n再放：\n蓝色染料×１\n煤炭块×１\n纯铁或纯金刀镡×１\n烈焰棒×１\n金锭×１\n成品刀条×１\n\n用锻造锤右键完成。",
            "红玉\n\n先放：银纸竹光×１\n再放：\n耀魂铁锭×１\n耀魂碎片×１\n红色染料×１\n纯铁或纯金刀镡×１\n成品刀条×１\n\n用锻造锤右键完成。\n耀魂碎片请对照\nJEI 中的图标选取。",
            "升级时继承的养成\n\n新刀保留原刀的杀敌数、\n耀魂数和精炼数。\n\n攻击以原刀基础攻击为底，\n加上目标刀较高的基础\n攻击差值，再加本次\n刀条与刀镡的攻击修正。\n\n精炼加成由拔刀剑计算，\n成刀使用目标刀的外观\n与招式。",
            "耀魂强化与修复\n\n先把刀放上空锻造铁砧，\n再放一件耀魂材料，\n用锻造锤右键吸收。\n\n每次精炼数＋１，\n增加耀魂、杀敌并修复。\n基础攻击增量随精炼\n次数增多而逐渐减少。\n吸收后可继续放下一件，\n空手右键取回刀和材料。",
            "耀魂材料的效果\n\n耀魂碎片：耀魂＋２００\n杀敌＋１，恢复耐久１０\n\n耀魂铁锭：耀魂＋４００\n杀敌＋２，恢复耐久２０\n\n耀魂宝珠：耀魂＋１０００\n杀敌＋５，恢复耐久５０\n\n耐久最多恢复到满值。\n铁砧接受这三种耀魂材料。",
            "附属刀剑的制作\n\n本体与附属的工作台、\n锻造台拔刀剑配方，\n自动由刀剑制作台接管。\n\n原材料与原刀条件保留，\n并补入成品刀条与\n纯铁或纯金刀镡各一件。\n\n安装附属后进入存档，\n在 JEI 查看完整清单。",
            "精炼与伤害增长\n\n精炼计数照常保留。\n精炼带来的伤害按\n递减曲线增长，不再线性\n堆高；本体的段位门槛\n及玩家等级限制仍保留。\n\n耀魂锻打首次数值约\n增加０．２４攻击，\n精炼越高，单次越少。\n杀敌与耀魂增长不受影响。",
            "材料取回与操作提示\n\n刀剑制作台：蹲下后\n空手右键可取回全部材料。\n名刀材料未齐时，也可\n直接空手右键取回。\n\n锻造铁砧：空手右键\n取回材料，再换其他工序。\n刀镡制作台：完成加工后\n空手右键取出成品。\n烧铁炉：烧好后用钳子取。",
            "致谢\n\n检视功能原作者：\n猫叩MewKooo\n\n感谢猫叩MewKooo\n公开分享检视模组\n与动画，本模组的\n检视功能在其基础\n上改进并制作。\n\n再次向原作者与\n贡献者致谢。"
    );

    public GuideBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 原版 ServerPlayer#openItemGui 只在物品等于 Items.WRITTEN_BOOK 时才下发开书包，
     * 自定义成书类物品右键永远打不开。这里自己在服务端补发 ClientboundOpenBookPacket，
     * 客户端收到后按物品上的 WRITTEN_BOOK_CONTENT 组件打开书页。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 老存档中的手册也在打开时更新，无需丢弃再领取。
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                    written(this).get(DataComponents.WRITTEN_BOOK_CONTENT));
            serverPlayer.containerMenu.broadcastChanges();
            if (WrittenBookItem.resolveBookComponents(stack, serverPlayer.createCommandSourceStack(), serverPlayer)) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 生成一本写好的手册（成书内容挂在 WRITTEN_BOOK_CONTENT 组件上）。 */
    public static ItemStack written(Item item) {
        ItemStack stack = new ItemStack(item);
        List<Filterable<Component>> pages = new ArrayList<>(PAGES.size());
        for (String page : PAGES) {
            pages.add(Filterable.passThrough(Component.literal(page)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(TITLE), AUTHOR, 0, pages, true));
        return stack;
    }
}
