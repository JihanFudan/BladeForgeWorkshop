package cn.blockforge.generated.slashbladereshslashblad;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class BladeData {
    private static final String ROOT = "SlashBladeResharpedForge";
    private static final String BLADE = "Blade";
    private static final String DAMAGE = "DamageBonus";
    private static final String DURABILITY = "DurabilityBonus";
    private static final String GUARD = "Guard";
    private static final String FOLDS = "SoulFolds";
    private static final String SOUL_ATTACK = "SoulAttackBonus";
    private static final String RATIO = "ForgeRatio";
    private static final String RATIO_IRON = "Iron";
    private static final String RATIO_STEEL = "Steel";
    private static final String HOT = "HotSince";

    /** 钢锭数量 0..5 对应的攻击/耐久修正（钢多更锋利，铁-more 更耐用）。 */
    private static final int[] RATIO_ATTACK = {-3, -2, 1, 2, 4, 5};
    private static final int[] RATIO_DURABILITY = {5, 5, 5, 3, 0, -5};

    public static final ResourceLocation SILVERBAMBOO =
            ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade_silverbamboo");
    public static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade_white");
    public static final ResourceLocation MUMEI =
            ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade");
    public static final ResourceLocation RUBY =
            ResourceLocation.fromNamespaceAndPath("slashblade", "ruby");
    public static final ResourceLocation SLASHBLADE_WOOD =
            ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade_wood");
    public static final ResourceLocation SLASHBLADE_BAMBOO =
            ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade_bamboo");
    private static final String BLADE_STATE = "bladeState";
    private static final String BASE_ATTACK = "baseAttackModifier";
    private static final String MAX_DAMAGE = "maxDamage";
    private static final String CURRENT_DAMAGE = "Damage";

    private static final ResourceLocation PROUDSOUL = ResourceLocation.fromNamespaceAndPath("slashblade", "proudsoul");
    private static final ResourceLocation PROUDSOUL_INGOT = ResourceLocation.fromNamespaceAndPath("slashblade", "proudsoul_ingot");
    private static final ResourceLocation PROUDSOUL_SPHERE = ResourceLocation.fromNamespaceAndPath("slashblade", "proudsoul_sphere");

    private BladeData() {
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static CompoundTag root(ItemStack stack) {
        CompoundTag root = data(stack);
        if (!root.contains(ROOT)) {
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }

    private static void save(ItemStack stack, CompoundTag root) {
        CompoundTag data = data(stack);
        data.put(ROOT, root);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    private static CompoundTag externalState(ItemStack stack) {
        return SlashBladeBridge.read(stack);
    }

    private static void saveExternalState(ItemStack stack, CompoundTag state) {
        SlashBladeBridge.write(stack, state);
    }

    public static boolean isSlashBlade(ItemStack stack) {
        return SlashBladeBridge.isBlade(stack);
    }

    public static boolean isProudSoul(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return PROUDSOUL.equals(id) || PROUDSOUL_INGOT.equals(id) || PROUDSOUL_SPHERE.equals(id);
    }

    /** 锻造铁砧允许的三档耀魂材料：耀魂碎片、耀魂铁锭、耀魂宝珠。 */
    public static int proudSoulValue(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (PROUDSOUL.equals(id)) return 200;
        if (PROUDSOUL_INGOT.equals(id)) return 400;
        if (PROUDSOUL_SPHERE.equals(id)) return 1000;
        return 0;
    }

    public static int proudSoulKills(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (PROUDSOUL.equals(id)) return 1;
        if (PROUDSOUL_INGOT.equals(id)) return 2;
        if (PROUDSOUL_SPHERE.equals(id)) return 5;
        return 0;
    }

    public static int proudSoulDurability(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (PROUDSOUL.equals(id)) return 10;
        if (PROUDSOUL_INGOT.equals(id)) return 20;
        if (PROUDSOUL_SPHERE.equals(id)) return 50;
        return 0;
    }

    public static int damageBonus(ItemStack stack) {
        return root(stack).getInt(DAMAGE);
    }

    public static int durabilityBonus(ItemStack stack) {
        return root(stack).getInt(DURABILITY);
    }

    public static void markBlade(ItemStack stack, int damage, int durability, String guard) {
        CompoundTag root = root(stack);
        root.putInt("DataVersion", 2);
        root.putBoolean(BLADE, true);
        root.putInt(DAMAGE, damage);
        root.putInt(DURABILITY, durability);
        root.putString(GUARD, guard);
        save(stack, root);
    }

    /**
     * 将本附属的材质修正写入 SlashBlade Resharped 实际使用的 bladeState。
     * 该方法不依赖对方的 Forge 类，因此在没有开发依赖时也能编译。
     */
    public static void markCompatibleBlade(ItemStack stack, int damage, int durability, String guard, boolean bamboo) {
        CompoundTag state = externalState(stack);
        float baseAttack = state.contains(BASE_ATTACK) ? state.getFloat(BASE_ATTACK) : (bamboo ? 3.0f : 2.0f);
        int maxDamage = state.contains(MAX_DAMAGE) ? state.getInt(MAX_DAMAGE) : (bamboo ? 70 : 60);
        state.putFloat(BASE_ATTACK, Math.max(1.0f, baseAttack + damage));
        state.putInt(MAX_DAMAGE, Math.max(1, maxDamage + durability));
        state.putInt(CURRENT_DAMAGE, 0);
        state.putBoolean("isBroken", false);
        state.putBoolean("isSealed", false);
        state.putBoolean("isDefaultBewitched", false);
        saveExternalState(stack, state);
        markBlade(stack, damage, durability, guard);
    }

    /** 选择 SlashBladeResharped 注册的真实基础刀；本体是必需前置，不再生成替代刀。 */
    public static ItemStack createCompatibleBlade(int damage, int durability, String guard, boolean bamboo) {
        ResourceLocation bladeId = bamboo ? SLASHBLADE_BAMBOO : SLASHBLADE_WOOD;
        Item item = BuiltInRegistries.ITEM.get(bladeId);
        ItemStack result = new ItemStack(item);
        markCompatibleBlade(result, damage, durability, guard, bamboo);
        return result;
    }

    /** 只迁移本附属旧版标记的刀；不改玩家的其他拔刀剑。 */
    public static void migrateLegacy(ItemStack stack) {
        if (!isSlashBlade(stack)) return;
        CompoundTag root = root(stack);
        if (!root.getBoolean(BLADE) || root.getInt("DataVersion") >= 2) return;
        CompoundTag state = externalState(stack);
        CompoundTag old = data(stack).getCompound(BLADE_STATE);
        boolean basic = isSword(stack, SLASHBLADE_WOOD) || isSword(stack, SLASHBLADE_BAMBOO);
        float base = state.getFloat(BASE_ATTACK);
        if (basic && old.contains(BASE_ATTACK)) {
            state.putFloat(BASE_ATTACK, Math.max(1f, old.getFloat(BASE_ATTACK)));
            if (old.contains(MAX_DAMAGE)) state.putInt(MAX_DAMAGE, Math.max(1, old.getInt(MAX_DAMAGE)));
        } else {
            state.putFloat(BASE_ATTACK, Math.max(1f, base + root.getInt(DAMAGE)));
        }
        // 删除旧版附属写入的静态攻击项，保留其他模组写入的条目。
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            var entries = modifiers.modifiers().stream().filter(entry ->
                    !entry.modifier().id().equals(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "upgrade_attack"))).toList();
            if (entries.isEmpty()) stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            else stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    new net.minecraft.world.item.component.ItemAttributeModifiers(entries, modifiers.showInTooltip()));
        }
        saveExternalState(stack, state);
        root.putInt("DataVersion", 2);
        save(stack, root);
    }

    /** 增加攻击、耀魂和击杀数，并恢复已损耗的耐久，不改变耐久上限。 */
    public static void addSoulFold(ItemStack stack, int soul, int kills, int repairAmount) {
        migrateLegacy(stack);
        CompoundTag root = root(stack);
        root.putBoolean(BLADE, true);
        root.putInt("DataVersion", 2);
        root.putInt(FOLDS, saturatedAdd(root.getInt(FOLDS), 1));
        if (isSlashBlade(stack)) {
            CompoundTag state = externalState(stack);
            float gain = RefinementCurve.nextSoulGain(state.getInt("RepairCounter"));
            root.putFloat(SOUL_ATTACK, root.getFloat(SOUL_ATTACK) + gain);
            save(stack, root);
            state.putFloat(BASE_ATTACK, state.getFloat(BASE_ATTACK) + gain);
            state.putInt("proudSoul", saturatedAdd(state.getInt("proudSoul"), soul));
            state.putInt("killCount", saturatedAdd(state.getInt("killCount"), kills));
            state.putInt(CURRENT_DAMAGE, Math.max(0, state.getInt(CURRENT_DAMAGE) - Math.max(0, repairAmount)));
            state.putInt("RepairCounter", saturatedAdd(state.getInt("RepairCounter"), 1));
            state.putBoolean("isBroken", false);
            saveExternalState(stack, state);
        }
    }

    private static int saturatedAdd(int value, int increment) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, value) + Math.max(0, increment));
    }

    /* ---------------- 锻造链：锭材配比 / 灼热时间 ---------------- */

    /** 在物品上记录五枚锭材的铁/钢配比，一路从融合钢带到成品刀条。 */
    public static void putRatio(ItemStack stack, int iron, int steel) {
        CompoundTag data = data(stack);
        CompoundTag ratio = new CompoundTag();
        ratio.putInt(RATIO_IRON, iron);
        ratio.putInt(RATIO_STEEL, steel);
        data.put(RATIO, ratio);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    public static boolean hasRatio(ItemStack stack) {
        return data(stack).contains(RATIO);
    }

    public static int ratioIron(ItemStack stack) {
        CompoundTag ratio = data(stack).getCompound(RATIO);
        return ratio.getInt(RATIO_IRON);
    }

    public static int ratioSteel(ItemStack stack) {
        CompoundTag ratio = data(stack).getCompound(RATIO);
        return ratio.getInt(RATIO_STEEL);
    }

    public static void copyRatio(ItemStack from, ItemStack to) {
        if (hasRatio(from)) {
            putRatio(to, ratioIron(from), ratioSteel(from));
        }
    }

    public static int ratioAttackBonus(ItemStack stack) {
        return RATIO_ATTACK[Math.clamp(ratioSteel(stack), 0, 5)];
    }

    public static int ratioDurabilityBonus(ItemStack stack) {
        return RATIO_DURABILITY[Math.clamp(ratioSteel(stack), 0, 5)];
    }

    /** 记录“出炉时刻”，烫手刀条靠它做 30 秒自然冷却。 */
    public static void setHotSince(ItemStack stack, long gameTime) {
        CompoundTag data = data(stack);
        data.putLong(HOT, gameTime);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    public static long hotSince(ItemStack stack) {
        return data(stack).getLong(HOT);
    }

    /* ---------------- 前置拔刀剑物品查询 ---------------- */

    /** 按注册名取 SlashBladeResharped 的真实刀剑；前置缺失时返回空栈。 */
    public static ItemStack sword(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static boolean isSword(ItemStack stack, ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item != net.minecraft.world.item.Items.AIR && stack.is(item);
    }

    /**
     * 只迁移养成数据，保留目标刀自身的模型、贴图、名称、招式和其他定义。
     * 攻击继承旧刀的实际基础攻击，再加上两种刀原始基础攻击的正向差值；
     * 精炼交给前置动态计算，不把精炼或玩家药水加成重复写进基础攻击。
     */
    public static void inheritProgress(ItemStack source, ItemStack target, ItemStack definition) {
        if (!isSlashBlade(source) || !isSlashBlade(target)) return;
        // 在副本上迁移旧版数据，不在产物完成前改动台上的原刀。
        ItemStack original = source.copy();
        migrateLegacy(original);
        CompoundTag sourceState = externalState(original);
        CompoundTag targetState = externalState(target);
        CompoundTag sourceDefaults = externalState(new ItemStack(source.getItem()));
        CompoundTag originalRoot = root(original);
        float sourceBase = originalRoot.contains("DefinitionAttack") ? originalRoot.getFloat("DefinitionAttack")
                : sourceDefaults.getFloat(BASE_ATTACK);
        float targetBase = externalState(definition).getFloat(BASE_ATTACK);
        float tierIncrease = Math.max(0.0f, targetBase - sourceBase);
        targetState.putFloat(BASE_ATTACK, Math.max(targetState.getFloat(BASE_ATTACK),
                Math.max(1.0f, sourceState.getFloat(BASE_ATTACK) + tierIncrease)));
        // 这三项是前置的原生养成计数，不是附属的锻打次数。
        for (String key : new String[]{"killCount", "proudSoul", "RepairCounter"}) {
            targetState.putInt(key, Math.max(targetState.getInt(key), sourceState.getInt(key)));
        }
        saveExternalState(target, targetState);

        // 保留累计锻打攻击和次数，避免连续升级时丢失历史记录。
        CompoundTag sourceRoot = root(original);
        CompoundTag targetRoot = root(target);
        targetRoot.putInt(DAMAGE, sourceRoot.getInt(DAMAGE));
        targetRoot.putInt(FOLDS, sourceRoot.getInt(FOLDS));
        targetRoot.putFloat(SOUL_ATTACK, sourceRoot.getFloat(SOUL_ATTACK));
        save(target, targetRoot);
    }

    public static void rememberDefinition(ItemStack blade, ItemStack definition) {
        CompoundTag root = root(blade);
        root.putFloat("DefinitionAttack", externalState(definition).getFloat(BASE_ATTACK));
        save(blade, root);
    }

    /**
     * 给制作台刀剑附加锻打修正：
     * 在继承后的真实基础值上叠加攻击和耐久修正，不重置当前耐久损耗。
     */
    public static void applyUpgradeBonus(ItemStack stack, int attack, int durability, String guard) {
        CompoundTag state = externalState(stack);
        float baseAttack = state.contains(BASE_ATTACK) ? state.getFloat(BASE_ATTACK) : 1.0f;
        int maxDamage = state.contains(MAX_DAMAGE) ? state.getInt(MAX_DAMAGE) : stack.getMaxDamage();
        state.putFloat(BASE_ATTACK, Math.max(1.0f, baseAttack + attack));
        state.putInt(MAX_DAMAGE, Math.max(1, maxDamage + durability));
        saveExternalState(stack, state);
        markBlade(stack, damageBonus(stack) + attack, durabilityBonus(stack) + durability, guard);
    }
}
