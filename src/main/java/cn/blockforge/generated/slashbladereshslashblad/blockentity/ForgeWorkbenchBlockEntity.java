package cn.blockforge.generated.slashbladereshslashblad.blockentity;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 刀镡制作台与锻造铁砧共用的方块实体。
 * 刀镡台：木/竹刀镡五步流程 + 纯金属刀镡两步流程（stage 6/7）。
 * 铁砧：只负责金属刀条锻造链——灼热锭材配比→融合钢→粗制→未完成→（覆土后）烫手刀条，
 *        以及成品的耀魂升级/修复。物品会悬浮显示在台面上（客户端渲染器负责）。
 */
public class ForgeWorkbenchBlockEntity extends BlockEntity {
    /** 工位动作的音效与粒子风格。 */
    public enum Kind { WOOD, BAMBOO, METAL }

    private final SimpleContainer inventory = new SimpleContainer(4);
    private int stage;
    private boolean bambooBase;
    private String guardMaterial = "";
    /** 纯金属刀镡路线标记："" 未开始，"iron"/"gold" 正在锻制纯铁/纯金刀镡（stage 6/7）。 */
    private String pureMetal = "";
    /** 最近一次锤击的游戏时间，客户端渲染器用它做“下压回弹”动画。 */
    private long lastHit;
    // 原料和已使用工具仅作工序回顾显示，不参与取出、掉落和合成。
    private ItemStack sourceDisplay = ItemStack.EMPTY;
    private ItemStack toolDisplay = ItemStack.EMPTY;

    public ItemStack sourceDisplay() { return sourceDisplay; }
    public ItemStack toolDisplay() { return toolDisplay; }
    public int stage() { return stage; }

    public ForgeWorkbenchBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static ForgeWorkbenchBlockEntity create(BlockPos pos, BlockState state) {
        return new ForgeWorkbenchBlockEntity(GeneratedMod.FORGE_WORKBENCH_ENTITY.get(), pos, state);
    }

    public SimpleContainer items() {
        return inventory;
    }

    public long lastHit() {
        return lastHit;
    }

    /** 方块被破坏/移除时，把暂存在工位上的材料掉出来。 */
    public void dropStoredItems(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, inventory);
    }

    /* ================================ 刀镡制作台 ================================ */

    public ItemInteractionResult interactTsuba(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 0 && isWoodOrBamboo(held)) {
            inventory.setItem(0, held.copyWithCount(1));
            sourceDisplay = held.copyWithCount(1);
            toolDisplay = ItemStack.EMPTY;
            bambooBase = held.is(Items.BAMBOO_PLANKS);
            held.shrink(1);
            stage = 1;
            tell(player, "木材已固定。下一步：使用切割刀加工刀条。");
            workEffect(level, pos, bambooBase ? Kind.BAMBOO : Kind.WOOD, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        // 纯金属刀镡路线：空台直接放 1 块铁锭/金锭，不走木竹工序。
        if (stage == 0 && (held.is(Items.IRON_INGOT) || held.is(Items.GOLD_INGOT))) {
            inventory.setItem(0, held.copyWithCount(1));
            sourceDisplay = held.copyWithCount(1);
            toolDisplay = ItemStack.EMPTY;
            pureMetal = inventory.getItem(0).is(Items.GOLD_INGOT) ? "gold" : "iron";
            held.shrink(1);
            stage = 6;
            tell(player, "金属锭已摆上制作台。下一步：使用雕刻凿锻打成型。");
            workEffect(level, pos, Kind.METAL, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 6 && held.is(GeneratedMod.CARVING_CHISEL.get())) {
            inventory.setItem(0, new ItemStack(pureMetal.equals("gold")
                    ? GeneratedMod.TSUBA_PURE_GOLD.get() : GeneratedMod.TSUBA_PURE_IRON.get()));
            toolDisplay = held.copyWithCount(1);
            stage = 7;
            tell(player, "刀镡已锻打成型！空手右键取出。");
            workEffect(level, pos, Kind.METAL, true);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 7 && held.isEmpty()) {
            giveBack(player, inventory.removeItemNoUpdate(0));
            stage = 0;
            sourceDisplay = ItemStack.EMPTY;
            toolDisplay = ItemStack.EMPTY;
            pureMetal = "";
            tell(player, "纯金属刀镡已取出。制作台已清空，可以开始下一件。");
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 1 && held.is(GeneratedMod.CUTTING_KNIFE.get())) {
            inventory.setItem(0, new ItemStack(bambooBase ? GeneratedMod.BAMBOO_BLADE_BLANK.get() : GeneratedMod.WOODEN_BLADE_BLANK.get()));
            toolDisplay = held.copyWithCount(1);
            stage = 2;
            tell(player, "刀条已切出。下一步：使用雕刻凿修整刀鞘与刀镡槽位。");
            workEffect(level, pos, bambooBase ? Kind.BAMBOO : Kind.WOOD, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 2 && held.is(GeneratedMod.CARVING_CHISEL.get())) {
            inventory.setItem(1, new ItemStack(GeneratedMod.BLADE_HANDLE.get()));
            inventory.setItem(2, new ItemStack(GeneratedMod.BLADE_SHEATH.get()));
            toolDisplay = held.copyWithCount(1);
            stage = 3;
            tell(player, "刀鞘与刀镡轮廓已雕刻。放入铁锭或金锭选择刀镡材质。");
            workEffect(level, pos, bambooBase ? Kind.BAMBOO : Kind.WOOD, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 3 && (held.is(Items.IRON_INGOT) || held.is(Items.GOLD_INGOT))) {
            guardMaterial = held.is(Items.GOLD_INGOT) ? "gold" : "iron";
            inventory.setItem(3, held.copyWithCount(1));
            held.shrink(1);
            stage = 4;
            tell(player, "刀镡材料已嵌入。再次使用雕刻凿完成刀镡。");
            workEffect(level, pos, Kind.METAL, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 4 && held.is(GeneratedMod.CARVING_CHISEL.get())) {
            ItemStack base = inventory.getItem(0);
            boolean bamboo = base.is(GeneratedMod.BAMBOO_BLADE_BLANK.get());
            Item result = bamboo
                    ? (guardMaterial.equals("gold") ? GeneratedMod.TSUBA_BAMBOO_GOLD.get() : GeneratedMod.TSUBA_BAMBOO_IRON.get())
                    : (guardMaterial.equals("gold") ? GeneratedMod.TSUBA_WOOD_GOLD.get() : GeneratedMod.TSUBA_WOOD_IRON.get());
            inventory.setItem(0, new ItemStack(result));
            inventory.setItem(1, new ItemStack(GeneratedMod.BLADE_HANDLE.get()));
            inventory.setItem(2, new ItemStack(GeneratedMod.BLADE_SHEATH.get()));
            sourceDisplay = inventory.getItem(3).copy();
            inventory.setItem(3, ItemStack.EMPTY);
            toolDisplay = held.copyWithCount(1);
            stage = 5;
            tell(player, "刀镡完成！取出刀镡，另配刀条（木/竹刀条或成品刀条）到刀剑制作台组装。");
            workEffect(level, pos, Kind.METAL, true);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (stage == 5 && held.isEmpty()) {
            giveBack(player, inventory.removeItemNoUpdate(0));
            giveBack(player, inventory.removeItemNoUpdate(1));
            giveBack(player, inventory.removeItemNoUpdate(2));
            stage = 0;
            sourceDisplay = ItemStack.EMPTY;
            toolDisplay = ItemStack.EMPTY;
            bambooBase = false;
            guardMaterial = "";
            pureMetal = "";
            tell(player, "刀镡、刀柄、刀鞘已取出。制作台已清空，可以开始下一件。");
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        tell(player, stageHint());
        return ItemInteractionResult.SUCCESS;
    }

    /* ================================ 锻造铁砧 ================================ */

    public ItemInteractionResult interactAnvil(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (anyOccupied()) {
                giveBack(player, inventory.removeItemNoUpdate(0));
                giveBack(player, inventory.removeItemNoUpdate(1));
                giveBack(player, inventory.removeItemNoUpdate(2));
                giveBack(player, inventory.removeItemNoUpdate(3));
                tell(player, "铁砧上的材料已取出。");
                sync();
            } else {
                tell(player, "锻造铁砧：灼热铁锭/钢锭共 5 枚锤成融合钢；烧红工件可继续锻打；刀剑配耀魂材料可强化并恢复耐久。");
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 锻造锤：全部锤击逻辑
        if (held.is(GeneratedMod.FORGING_HAMMER.get())) {
            strike(level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }
        // 刀剑升级、热工件和锭材配比三种操作互斥，防止材料混入别的工序。
        if ((held.is(GeneratedMod.HEATED_IRON.get()) || held.is(GeneratedMod.HEATED_STEEL.get()))
                && !inventory.getItem(0).isEmpty()) {
            tell(player, "先取出或完成当前工件，再放灼热锭材。");
            return ItemInteractionResult.SUCCESS;
        }
        if ((isHeatedWork(held) || BladeData.isSlashBlade(held)) && metalCount() > 0) {
            tell(player, "先完成或取出配比锭材，再放刀剑或工件。");
            return ItemInteractionResult.SUCCESS;
        }
        // 放灼热锭材（配比槽）
        if (held.is(GeneratedMod.HEATED_IRON.get())) {
            if (metalCount() >= 5) {
                tell(player, "配比槽已满（5/5），先锤第一锤。");
                return ItemInteractionResult.SUCCESS;
            }
            addHeated(1, held);
            tell(player, "灼热铁锭已放上铁砧：铁 " + ironCount() + " / 钢 " + steelCount() + "（共 " + metalCount() + "/5）。");
            workEffect(level, pos, Kind.METAL, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(GeneratedMod.HEATED_STEEL.get())) {
            if (metalCount() >= 5) {
                tell(player, "配比槽已满（5/5），先锤第一锤。");
                return ItemInteractionResult.SUCCESS;
            }
            addHeated(2, held);
            tell(player, "灼热钢锭已放上铁砧：铁 " + ironCount() + " / 钢 " + steelCount() + "（共 " + metalCount() + "/5）。");
            workEffect(level, pos, Kind.METAL, false);
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        // 放烧红工件（工序槽）
        if (isHeatedWork(held)) {
            if (inventory.getItem(0).isEmpty()) {
                inventory.setItem(0, held.copyWithCount(1));
                held.shrink(1);
                tell(player, workHint(inventory.getItem(0)));
                workEffect(level, pos, Kind.METAL, false);
                sync();
            } else {
                tell(player, "工序槽已被占用，先完成当前工件。");
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 刀剑升级/修复
        if (BladeData.isSlashBlade(held) && inventory.getItem(0).isEmpty()) {
            inventory.setItem(0, held.copyWithCount(1));
            held.shrink(1);
            tell(player, "刀剑已放上铁砧。放入耀魂材料后用锻造锤锤击可提升伤害。");
            sync();
            return ItemInteractionResult.SUCCESS;
        }
        if (BladeData.isProudSoul(held)) {
            if (BladeData.isSlashBlade(inventory.getItem(0))) {
                if (inventory.getItem(3).isEmpty()) {
                    inventory.setItem(3, held.copyWithCount(1));
                    held.shrink(1);
                    tell(player, "耀魂材料已放入。使用锻造锤将它吸收入刀身。");
                    sync();
                } else {
                    tell(player, "耀魂材料槽已占用，请先用锻造锤吸收当前材料。");
                }
            } else {
                tell(player, "耀魂材料已暂存。随后放入拔刀剑即可继续锻造。");
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 冷锭需要先加热
        if (held.is(Items.IRON_INGOT) || held.is(GeneratedMod.STEEL_INGOT.get())) {
            tell(player, "冷锭不能直接上砧。先放进烧铁炉烧成灼热锭材（烧红后要用钳子夹取）。");
            return ItemInteractionResult.SUCCESS;
        }
        tell(player, "铁砧不认识这件材料。金属刀条请从烧铁炉的灼热锭材开始。");
        return ItemInteractionResult.SUCCESS;
    }

    /** 锻造锤右键铁砧：按当前槽内容推进一道工序。 */
    private void strike(Level level, BlockPos pos, Player player) {
        ItemStack work = inventory.getItem(0);
        // 灼热融合钢 → 粗制刀条（第二锤）
        if (work.is(GeneratedMod.HEATED_FUSED_STEEL.get())) {
            ItemStack out = new ItemStack(GeneratedMod.CRUDE_BLADE.get());
            BladeData.copyRatio(work, out);
            inventory.setItem(0, ItemStack.EMPTY);
            giveBack(player, out);
            tell(player, "第二锤成型：粗制刀条！再放回烧铁炉烧红，继续第三锤。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        // 灼热粗制刀条 → 未完成的刀条（第三锤）
        if (work.is(GeneratedMod.HEATED_CRUDE_BLADE.get())) {
            ItemStack out = new ItemStack(GeneratedMod.UNFINISHED_BLADE.get());
            BladeData.copyRatio(work, out);
            inventory.setItem(0, ItemStack.EMPTY);
            giveBack(player, out);
            tell(player, "第三锤成型：未完成的刀条！另一只手拿一团黏土、手持刀条右键烧铁炉，覆土后直接烧红。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        // 灼热覆土刀条 → 烫手的刀条（最后一锤）
        if (work.is(GeneratedMod.HEATED_CLAY_BLADE.get())) {
            ItemStack out = new ItemStack(GeneratedMod.HOT_BLADE.get());
            BladeData.copyRatio(work, out);
            BladeData.setHotSince(out, level.getGameTime());
            inventory.setItem(0, ItemStack.EMPTY);
            giveBack(player, out);
            tell(player, "淬火前的最后一锤完成！刀条烫手：右键有水炼药锅急速降温，或握在包里等 30 秒。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        // 刀剑 + 耀魂 → 吸收升级
        if (BladeData.isSlashBlade(work) && BladeData.isProudSoul(inventory.getItem(3))) {
            ItemStack soul = inventory.getItem(3);
            int soulValue = BladeData.proudSoulValue(soul);
            int kills = BladeData.proudSoulKills(soul);
            int repairAmount = BladeData.proudSoulDurability(soul);
            BladeData.addSoulFold(work, soulValue, kills, repairAmount);
            inventory.setItem(3, ItemStack.EMPTY);
            tell(player, "耀魂材料被锤入刀身：伤害增加，耀魂 +" + soulValue + "，击杀数 +" + kills + "，恢复耐久 " + repairAmount + " 点（最多恢复至满耐久）。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        // 单枚灼热铁锭 → 钢锭
        if (ironCount() == 1 && steelCount() == 0) {
            inventory.setItem(1, ItemStack.EMPTY);
            giveBack(player, new ItemStack(GeneratedMod.STEEL_INGOT.get()));
            tell(player, "灼热铁锭经过锤打炼成钢锭。想烧红它继续用，就放回烧铁炉。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        // 五枚灼热锭材 → 融合钢（第一锤）
        if (metalCount() == 5) {
            int iron = ironCount();
            int steel = steelCount();
            ItemStack out = new ItemStack(GeneratedMod.FUSED_STEEL.get());
            BladeData.putRatio(out, iron, steel);
            inventory.setItem(1, ItemStack.EMPTY);
            inventory.setItem(2, ItemStack.EMPTY);
            giveBack(player, out);
            tell(player, "第一锤：钢 " + steel + " 铁 " + iron + " 已融合为融合钢！放进烧铁炉烧红后回砧上敲第二锤。");
            finish(level, pos, player, Kind.METAL);
            return;
        }
        if (metalCount() > 0) {
            tell(player, "配比需要凑满 5 枚灼热锭材（铁/钢任意比例）。当前 " + metalCount() + "/5。");
        } else if (work.isEmpty()) {
            tell(player, "铁砧是空的。放灼热锭材（凑 5 枚）或烧红的工件，再动锤。");
        } else {
            tell(player, workHint(work));
        }
        workEffect(level, pos, Kind.METAL, false);
        sync();
    }

    private void finish(Level level, BlockPos pos, Player player, Kind kind) {
        lastHit = level.getGameTime();
        player.swing(InteractionHand.MAIN_HAND, true);
        workEffect(level, pos, kind, true);
        sync();
    }

    private void addHeated(int slot, ItemStack held) {
        ItemStack stored = inventory.getItem(slot);
        if (stored.isEmpty()) {
            inventory.setItem(slot, held.copyWithCount(1));
        } else {
            stored.grow(1);
        }
        held.shrink(1);
    }

    private boolean anyOccupied() {
        for (int i = 0; i < 4; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int metalCount() {
        return ironCount() + steelCount();
    }

    private int ironCount() {
        return inventory.getItem(1).is(GeneratedMod.HEATED_IRON.get()) ? inventory.getItem(1).getCount() : 0;
    }

    private int steelCount() {
        return inventory.getItem(2).is(GeneratedMod.HEATED_STEEL.get()) ? inventory.getItem(2).getCount() : 0;
    }

    private static boolean isHeatedWork(ItemStack stack) {
        return stack.is(GeneratedMod.HEATED_FUSED_STEEL.get())
                || stack.is(GeneratedMod.HEATED_CRUDE_BLADE.get())
                || stack.is(GeneratedMod.HEATED_CLAY_BLADE.get());
    }

    private static String workHint(ItemStack work) {
        if (work.is(GeneratedMod.HEATED_FUSED_STEEL.get())) {
            return "灼热融合钢已上砧。锻造锤敲第二锤，出粗制刀条。";
        }
        if (work.is(GeneratedMod.HEATED_CRUDE_BLADE.get())) {
            return "灼热粗制刀条已上砧。锻造锤敲第三锤，出未完成的刀条。";
        }
        if (work.is(GeneratedMod.HEATED_CLAY_BLADE.get())) {
            return "灼热覆土刀条已上砧。锻造锤敲最后一锤，出烫手的刀条。";
        }
        return "工件已上砧，使用锻造锤继续。";
    }

    /* ================================ 共用小工具 ================================ */

    private String stageHint() {
        if (stage == 6 && !pureMetal.isEmpty()) {
            return "请使用雕刻凿把四块金属锭锻打成刀镡。";
        }
        if (stage == 7 && !pureMetal.isEmpty()) {
            return "请空手右键取出纯金属刀镡。";
        }
        return switch (stage) {
            case 1 -> "请使用切割刀加工刀条。";
            case 2 -> "请使用雕刻凿加工刀鞘与刀镡槽位。";
            case 3 -> "请放入铁锭或金锭选择刀镡材质。";
            case 4 -> "请使用雕刻凿完成刀镡。";
            case 5 -> "请空手取出刀镡和刀柄。";
            default -> "请先放入木板或竹板做木质刀镡，或放入 1 块铁锭/金锭锻制纯金属刀镡。";
        };
    }

    private static boolean isWoodOrBamboo(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.PLANKS);
    }

    private static void tell(Player player, String text) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    /** 木质工序放削木声，金属工序放打铁声；done=true 表示一道工序完成。 */
    private static void workEffect(Level level, BlockPos pos, Kind kind, boolean done) {
        SoundEvent hit = switch (kind) {
            case WOOD -> SoundEvents.AXE_STRIP;
            case BAMBOO -> SoundEvents.BAMBOO_BREAK;
            case METAL -> done ? SoundEvents.SMITHING_TABLE_USE : SoundEvents.ANVIL_USE;
        };
        level.playSound(null, pos, hit, SoundSource.BLOCKS, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
        if (done) {
            level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.7f, 1.0f);
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(done ? ParticleTypes.CRIT : ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    done ? 8 : 3, 0.15, 0.08, 0.15, done ? 0.06 : 0.01);
        }
    }

    /** 库存/工序变化后：存盘 + 同步给附近客户端（模型显示靠它）。 */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stage = tag.getInt("Stage");
        bambooBase = tag.getBoolean("BambooBase");
        guardMaterial = tag.getString("Guard");
        pureMetal = tag.getString("PureMetal");
        lastHit = tag.getLong("LastHit");
        sourceDisplay = ItemStack.parseOptional(registries, tag.getCompound("SourceDisplay"));
        toolDisplay = ItemStack.parseOptional(registries, tag.getCompound("ToolDisplay"));
        inventory.setItem(0, ItemStack.parseOptional(registries, tag.getCompound("Slot0")));
        inventory.setItem(1, ItemStack.parseOptional(registries, tag.getCompound("Slot1")));
        inventory.setItem(2, ItemStack.parseOptional(registries, tag.getCompound("Slot2")));
        inventory.setItem(3, ItemStack.parseOptional(registries, tag.getCompound("Slot3")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stage", stage);
        tag.putBoolean("BambooBase", bambooBase);
        tag.putString("Guard", guardMaterial);
        tag.putString("PureMetal", pureMetal);
        tag.putLong("LastHit", lastHit);
        tag.put("SourceDisplay", sourceDisplay.saveOptional(registries));
        tag.put("ToolDisplay", toolDisplay.saveOptional(registries));
        // 1.21.1 中空物品栈调用 save() 会抛 IllegalStateException（拆方块同步数据时必崩），必须用 saveOptional。
        tag.put("Slot0", inventory.getItem(0).saveOptional(registries));
        tag.put("Slot1", inventory.getItem(1).saveOptional(registries));
        tag.put("Slot2", inventory.getItem(2).saveOptional(registries));
        tag.put("Slot3", inventory.getItem(3).saveOptional(registries));
    }
}
