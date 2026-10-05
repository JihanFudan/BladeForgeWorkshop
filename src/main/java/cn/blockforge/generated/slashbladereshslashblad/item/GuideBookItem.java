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
 * 玩家初次进入存档时自动发放一本（见 {@code PlayerBookEvents}）；之后想再要，
 * 用「书 + 锻造锤」无序合成即可，锻造锤在这里只当工具，不会被消耗。
 *
 * <p>页面版式硬限制（原版 BookViewScreen）：每页最多渲染
 * {@code TEXT_HEIGHT(128) / font.lineHeight(9) = 14} 行、每行宽 114 像素，
 * 超出的行会被直接丢掉（不报错、不显示省略号）。为留出余量，这里约定
 * <b>每页不超过 13 行、每行不超过 12 个全角字</b>，改文案后请用
 * {@code animation_tools/book_layout_check.py} 复查一遍。</p>
 */
public class GuideBookItem extends WrittenBookItem {
    public static final String TITLE = "锻刀工坊游玩手册";
    public static final String AUTHOR = "jihanfudan";

    /** 成书页面一行最多约 12 个汉字，每页最多 13 行，保证书页不裁字；\n 在书页里正常换行。 */
    private static final List<String> PAGES = List.of(
            "锻刀工坊游玩手册\n\n本手册按实际操作顺序，\n讲解工具、工位、刀条、\n刀镡、钢材与刀剑名刀的\n制作和强化方法。\n\n后面还有寒霜的组装提示\n与各名刀的材料清单，\n以及升级、精炼、检视和\n取回材料的方法。",
            "制作入门\n\n本模组用四个专用工位，\n完成刀条、刀镡和刀剑\n的制作与强化。\n\nJEI 是查看配方的模组。\n安装后可查看完整的\n制作方法与材料数量。\n\n初次进入世界会自动发一\n本手册；书加锻造锤合成\n可得副本，锤子不消耗。",
            "如何查询配方\n\n装了 JEI 后，鼠标指向\n物品按 R 查制作方法，\n按 U 查它的用途。\n\n名刀配方在「刀剑制作\n台·名刀锻造」分类里，\n格子角标就是需要的\n材料数量。\n\n刀镡槽轮换显示纯铁、\n纯金、纯铜三选一。",
            "准备工具与工位\n\n在工作台合成切割刀、\n雕刻凿、锻造锤和钳子。\n切割刀与凿加工木竹，\n锤用于锻打，钳子防烫。\n\n再合成并放置四种工位：\n刀镡制作台、烧铁炉、\n锻造铁砧、刀剑制作台。\n\n工具和工位的摆法都可\n以在 JEI 里查到。",
            "先认识操作方式\n\n拿材料右键工位放入，\n换工具右键推进工序，\n提示写在快捷栏上方。\n\n双手加工：一手拿工具，\n一手拿材料，主副不限。\n副手格在人物模型右边。\n\n材料没放齐时别急着锤，\n空手右键可取回重放。",
            "制作刀镡与配件\n\n在空的刀镡制作台上：\n① 放一块木板或竹板\n② 用切割刀右键一次\n③ 用雕刻凿右键一次\n④ 放一枚铁、金或铜锭\n⑤ 再用雕刻凿右键一次\n\n空手右键，一次取出刀\n镡、刀柄和刀鞘三件。\n\n刀条要按下一页另做。",
            "制作木刀条或竹刀条\n\n一手拿切割刀，另一手\n拿木板（或竹板），对着\n空气右键，得到刀条雏形。\n\n把雏形拿在一只手里，\n另一只手换雕刻凿右键，\n得到木刀条或竹刀条。\n\n木条做木偶，竹条做竹光。",
            "组装木偶与竹光\n\n手持木刀条或竹刀条对空\n气右键：背包里有对应的\n刀镡、刀柄、刀鞘各一件\n时，播约 4 秒动画出刀。\n\n木偶用木铁／木金／铜木\n刀镡，竹光用竹铁／竹金\n／铜竹刀镡。\n\n三种都有时按金＞铜＞铁\n挑，别误放贵的刀镡。",
            "制作纯金属刀镡\n\n手持一枚铁锭、金锭或\n铜锭，右键空的刀镡制\n作台，消耗一枚锭材。\n\n用雕刻凿右键加工，再\n空手右键取出一枚纯铁、\n纯金或纯铜刀镡。\n\n这条路线只出刀镡，不\n出刀柄和刀鞘；名刀要用\n的就是这三种纯金属镡。",
            "组装名刀·寒霜\n\n寒霜不在刀剑制作台锻造，\n要从成品刀条开始组装。\n备好雪块、刀装与配件后，\n手持刀条对空气右键即可。\n\n缺少材料时会给出提示；\n完整清单请在 JEI 中点击\n寒霜本体查看信息栏。\n\n组装完成后仍可照常附魔、\n精炼与修复。",
            "烧铁炉与防烫\n\n右键炉子放入铁锭、钢锭、\n低碳钢、高碳钢、融合钢、\n粗制刀条或覆土刀条，一\n批最多 5 个，约 8 秒烧\n红，不用另加燃料。\n\n烧好后背包里有钳子就能\n右键夹出；没钳子时拿着\n烧红工件每秒烫 1 滴血。\n\n这批取完，才能放下批。",
            "过火与回炉\n\n粗制、覆土刀条烧红后\n超过 5 秒没夹出，颜色\n渐变成橙黄并过火，成\n为灼热的失败的刀条。\n\n空手拿会烫手掉血；等\n15 秒自然冷却或用水炼\n药锅降温，得到失败的\n刀条。回烧固定出 3 枚\n铁锭。锭材不限时。",
            "先炼钢，再研磨碳粉\n\n铁锭在烧铁炉里烧成灼\n热铁锭（原版熔炉也能烧\n这一种）。\n只放一枚灼热铁锭到空\n锻造铁砧，用锻造锤右\n键，炼成一枚钢锭。\n\n手持煤炭或木炭右键同\n一座铁砧放上一块，再\n用锻造锤右键研磨，得\n到 4 份碳粉。",
            "锻打两种碳钢\n\n在刀剑制作台台面上逐\n件右键放入材料，放齐\n后手持锻造锤右键锻打。\n\n低碳钢＝钢锭×1＋黏土\n球×1＋碳粉×1\n高碳钢＝钢锭×1＋黏土\n球×1＋碳粉×2\n\n台面只认这三样原料，\n不能与名刀材料混放。",
            "融合钢的第一锤\n\n把钢锭、低碳钢、高碳\n钢各烧成灼热锭材，在\n锻造铁砧上逐枚右键放\n入，三种随意混放，凑\n满 5 枚。\n\n手持锻造锤右键，得到\n一块融合钢；不足 5 枚\n时不会开锤。提示会写\n出当前枚数与攻耐修正。",
            "从融合钢到刀条\n\n第二锤：融合钢入炉烧\n红，用钳子取出灼热融\n合钢，放上空铁砧，锻\n造锤右键得到粗制刀条。\n\n第三锤：粗制刀条同样\n烧红、取出、上砧锤击，\n得到未完成的刀条。\n\n每一锤都会把钢材记录\n和攻耐修正一路带下去。",
            "覆土与最后一锤\n\n主手拿未完成的刀条，\n另一只手拿一个黏土球，\n右键空的烧铁炉：消耗\n一团黏土完成覆土，并\n直接在炉内加热，约 8\n秒后用钳子夹出灼热覆\n土刀条。\n\n放上空锻造铁砧，用锻\n造锤右键最后一锤，得\n到烫手的刀条。",
            "淬火得到成品刀条\n\n手持烫手的刀条，右键\n装有水的炼药锅，立即\n得到成品刀条。\n\n也可留在背包里，从最\n后一锤起等约 30 秒自\n然冷却成成品刀条。\n\n两种方式都保留钢材记\n录，成品刀条用于后面\n的名刀配方。",
            "钢材种类与属性\n\n每种钢单枚的基数：\n钢 攻+2 耐+2\n低碳钢 攻+1 耐+3\n高碳钢 攻+3 耐+1\n\n混熔按各钢枚数加权平均\n后四舍五入。例：钢×2、\n高碳钢×3 攻+3 耐+1。\n\n这份修正一路保留，最后\n再与刀镡修正相加。",
            "刀镡使用规则\n\n木偶：木铁／木金／铜木\n竹光：竹铁／竹金／铜竹\n其余：纯铁／纯金／纯铜\n\n修正只看金属，与是不是\n纯金属无关：\n铁 攻+0 耐+5\n金 攻+2 耐+1\n铜 攻+1 耐+1\n\n总修正＝刀条钢材＋刀镡。",
            "名刀升级的操作\n\n在刀剑制作台上逐件右\n键放入材料，次序不限，\n但必须有前置刀，也就\n是这次要用的那把刀。\n\n齐全后用锻造锤右键，\n消耗刀与材料得到新刀，\n成刀直接进入背包。\n没配齐时空手右键，\n可取回全部材料。",
            "银纸竹光\n\n先放：竹光×1\n再放：成品刀条×1\n纯金属刀镡×1\n纸×1\n黑色染料×1\n\n材料齐全后，用锻造锤\n右键完成。",
            "利刀白鞘\n\n先放：木偶×1\n再放：耀魂铁锭×2\n金锭×1\n成品刀条×1\n纯金属刀镡×1\n\n材料齐全后，用锻造锤\n右键完成。",
            "无铭「无名」\n\n先放：利刀白鞘×1\n再放：蓝色染料×1\n煤炭块×1\n纯金属刀镡×1\n烈焰棒×1\n金锭×1\n成品刀条×1\n\n用锻造锤右键完成。",
            "「无名」红玉\n\n先放：银纸竹光×1\n再放：耀魂铁锭×1\n耀魂碎片×1\n红色染料×1\n纯金属刀镡×1\n成品刀条×1\n\n用锻造锤右键完成；确\n切图标请以 JEI 清单为\n准。",
            "升级时继承的养成\n\n新刀保留原刀的杀敌数、\n耀魂数和精炼数。\n\n攻击以原刀当前的基础攻\n击为底，加上目标刀较高\n的基础攻击差值，再叠加\n本次刀条与刀镡的修正。\n\n精炼加成由拔刀剑本体计\n算，成刀使用目标刀的模\n型、外观与招式。",
            "耀魂强化与修复\n\n刀放上空锻造铁砧，再放\n一件耀魂材料，用锻造锤\n右键尝试吸收入刀身。\n\n成功会完成本次强化，并\n让下一锤失手率增加 1%。\n失手不加伤害、精炼与杀\n敌，耀魂和修复效果减半，\n但下一锤失手率降低 1%。\n概率最低为零，最高五成，\n放材料时会显示当前风险。",
            "耀魂材料的效果\n\n耀魂碎片：+400 耀魂\n杀敌+1，恢复耐久 20\n\n耀魂铁锭：+600 耀魂\n杀敌+2，恢复耐久 30\n\n耀魂宝珠：+1200 耀魂\n杀敌+5，恢复耐久 60\n\n耐久最多恢复到满值；\n铁砧只认这三种耀魂。",
            "精炼与刀剑成长\n\n耀魂强化会让刀剑逐步成\n长，但越往后提升越平缓，\n不会一直按同样幅度增加。\n\n玩家等级、刀剑段位与精\n炼门槛仍按拔刀剑本体规\n则生效；本模组只让后期\n成长更加稳定。\n\n想继续强化时，把刀与耀\n魂材料放上锻造铁砧即可。",
            "检视与究极次元斩\n\n手持任意拔刀剑按检视键\n（默认 G）播检视动画，\n再按一次或开界面打断。\n\n阎魔与寒霜可用 SSA：\n同按前进与后退，再按住\n右键蓄力 1 秒释放，\n耗两千、冷却 15 秒。\n只斩敌对生物；被斩的\n怪轻轻浮起慢慢飘落，\n落地才倒下。",
            "附属刀剑的制作\n\n本体与附属模组的工作\n台、锻造台拔刀剑配方，\n自动由刀剑制作台接管。\n\n原材料与原刀条件保留，\n并补入成品刀条与纯金\n属刀镡各一件。\n\n装好附属后进入存档，\n在 JEI 查看完整清单。",
            "材料取回与操作提示\n\n刀剑制作台：空手右键\n就能取回台上全部材料，\n名刀材料没齐时也一样。\n\n锻造铁砧：空手右键取\n回材料，再换其他工序。\n\n刀镡制作台：加工完成\n后空手右键取出成品。\n\n烧铁炉要用钳子夹取。",
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
