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
    private static final String RATIO_GRADE = "Grade";
    /** 融合时每种钢的枚数（1.0.4-r18 起支持混熔）。旧存档只有 Grade 一项，照常按单钢结算。 */
    private static final String RATIO_STEEL = "Steel";
    private static final String RATIO_LOW_CARBON = "LowCarbon";
    private static final String RATIO_HIGH_CARBON = "HighCarbon";
    private static final String[] RATIO_KEYS = {RATIO_STEEL, RATIO_LOW_CARBON, RATIO_HIGH_CARBON};
    private static final String HOT = "HotSince";

    /**
     * 金属种类（融合钢与后续工件携带，决定成品刀的攻击/耐久上限修正）。
     * 1.0.4-r17 起：融合配方去掉铁，改用三种钢——钢、低碳钢、高碳钢。
     */
    public static final int GRADE_STEEL = 0;
    public static final int GRADE_LOW_CARBON = 1;
    public static final int GRADE_HIGH_CARBON = 2;
    /**
     * 每种金属单枚给的基数修正（1.0.4-r18 按用户要求定的基数）：
     * 钢 攻2/耐2、低碳钢 攻1/耐3、高碳钢 攻3/耐1。
     * 混熔时按各钢枚数加权平均、四舍五入，所以五枚同种钢就是这份基数本身。
     */
    private static final int[] GRADE_ATTACK = {2, 1, 3};
    private static final int[] GRADE_DURABILITY = {2, 3, 1};
    private static final String[] GRADE_NAME = {"钢", "低碳钢", "高碳钢"};

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

    /**
     * 读出物品自定义 NBT 的一份可改副本。
     * <p>1.21.1 的 {@code CustomData} 是不可变快照：必须先 {@code copyTag()}，
     * 改完再 {@link CustomData#set} 写回去。只拿 {@code copyTag()} 改了却不
     * {@code set}，改动会丢；反过来，{@code contains} 必须对着这份副本问，
     * 不能对着空快照问——否则刚写入的配比会被读成"没有记录"，
     * 攻击/耐久修正就会一律显示 0。</p>
     */
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
        if (!root.getBoolean(BLADE)) return;
        if (root.getInt("DataVersion") < 2) {
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
        }
        // v3：修回被"无配比 -3 攻 / +5 耐"误伤的木/竹成品刀（本附属标记、记录里攻击加成为负即是旧产物）。
        // 旧铁镡木偶当时被 1.0 保底截断过，简单回补差值会和金镡撞车，所以按新规则直接重算：
        // 新基础攻击 = 出厂值（木偶 2.0 / 竹光 3.0）+ 本次刀镡修正（金 +2 / 铁 0）+ 耀魂锻打累计增量。
        // 耀魂增量取"现值减去旧规则应得值"反推——旧规则被保底截断的部分不计入，宁可少补不可过补。
        if (root.getInt("DataVersion") < 3 && root.getInt(DAMAGE) < 0
                && (isSword(stack, SLASHBLADE_WOOD) || isSword(stack, SLASHBLADE_BAMBOO))) {
            boolean gold = "gold".equals(root.getString(GUARD));
            boolean bamboo = isSword(stack, SLASHBLADE_BAMBOO);
            float vanilla = bamboo ? 3.0f : 2.0f;
            CompoundTag state = externalState(stack);
            float soulGain = Math.max(0.0f, state.getFloat(BASE_ATTACK) - Math.max(1.0f, vanilla + root.getInt(DAMAGE)));
            state.putFloat(BASE_ATTACK, Math.max(1.0f, vanilla + (gold ? 2.0f : 0.0f) + soulGain));
            int durabilityShrink = Math.max(0, root.getInt(DURABILITY) - (gold ? 1 : 5));
            state.putInt(MAX_DAMAGE, Math.max(1, state.getInt(MAX_DAMAGE) - durabilityShrink));
            state.putInt(CURRENT_DAMAGE, Math.min(Math.max(0, state.getInt(CURRENT_DAMAGE)), state.getInt(MAX_DAMAGE)));
            saveExternalState(stack, state);
            root.putInt(DAMAGE, gold ? 2 : 0);
            root.putInt(DURABILITY, gold ? 1 : 5);
            root.putInt("DataVersion", 3);
        }
        save(stack, root);
    }

    /** 增加攻击、耀魂和击杀数，并恢复已损耗的耐久，不改变耐久上限。 */
    public static void addSoulFold(ItemStack stack, int soul, int kills, int repairAmount) {
        migrateLegacy(stack);
        CompoundTag root = root(stack);
        root.putBoolean(BLADE, true);
        // 已经迁移过的刀保持更高版本号，别让每次锻打把 v3 标记降级回 v2。
        root.putInt("DataVersion", Math.max(2, root.getInt("DataVersion")));
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

    /** 在融合钢上记录金属种类（钢/低碳钢/高碳钢），一路带到成品刀条。 */
    public static void putRatio(ItemStack stack, int grade) {
        CompoundTag data = data(stack);
        CompoundTag ratio = new CompoundTag();
        ratio.putInt(RATIO_GRADE, Math.clamp(grade, 0, 2));
        data.put(RATIO, ratio);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    /** 混熔记录：counts 按钢种（0 钢、1 低碳钢、2 高碳钢）记枚数，属性按枚数加权分配。 */
    public static void putMixedRatio(ItemStack stack, int[] counts) {
        CompoundTag data = data(stack);
        CompoundTag ratio = new CompoundTag();
        int dominant = 0;
        for (int g = 1; g < RATIO_KEYS.length; g++) {
            if (Math.max(0, counts[g]) > Math.max(0, counts[dominant])) dominant = g;
        }
        ratio.putInt(RATIO_GRADE, dominant);
        for (int g = 0; g < RATIO_KEYS.length; g++) {
            ratio.putInt(RATIO_KEYS[g], Math.max(0, counts[g]));
        }
        data.put(RATIO, ratio);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
    }

    public static boolean hasRatio(ItemStack stack) {
        CompoundTag data = data(stack);
        return data.contains(RATIO) && !data.getCompound(RATIO).isEmpty();
    }

    /** 金属种类：0 钢、1 低碳钢、2 高碳钢；没有记录时按钢处理。 */
    public static int ratioGrade(ItemStack stack) {
        return Math.clamp(data(stack).getCompound(RATIO).getInt(RATIO_GRADE), 0, 2);
    }

    public static String gradeName(int grade) {
        return GRADE_NAME[Math.clamp(grade, 0, 2)];
    }

    public static String ratioGradeName(ItemStack stack) {
        return isMixedRatio(stack) ? "融合钢" : GRADE_NAME[ratioGrade(stack)];
    }

    /** 是否两种及以上钢材混熔而成。 */
    public static boolean isMixedRatio(ItemStack stack) {
        CompoundTag ratio = data(stack).getCompound(RATIO);
        if (!ratio.contains(RATIO_STEEL)) return false;
        int parts = 0;
        for (String key : RATIO_KEYS) {
            if (ratio.getInt(key) > 0) parts++;
        }
        return parts >= 2;
    }

    /** 配比明细，如“钢×2、高碳钢×3”；单钢或旧记录返回钢种名。 */
    public static String ratioMixSummary(ItemStack stack) {
        CompoundTag ratio = data(stack).getCompound(RATIO);
        if (!ratio.contains(RATIO_STEEL)) return ratioGradeName(stack);
        StringBuilder text = new StringBuilder();
        for (int g = 0; g < RATIO_KEYS.length; g++) {
            int count = ratio.getInt(RATIO_KEYS[g]);
            if (count <= 0) continue;
            if (!text.isEmpty()) text.append("、");
            text.append(GRADE_NAME[g]).append("×").append(count);
        }
        return text.isEmpty() ? GRADE_NAME[0] : text.toString();
    }

    public static void copyRatio(ItemStack from, ItemStack to) {
        CompoundTag ratio = data(from).getCompound(RATIO);
        if (ratio.isEmpty()) return;
        CompoundTag data = data(to);
        data.put(RATIO, ratio.copy());
        CustomData.set(DataComponents.CUSTOM_DATA, to, data);
    }

    /**
     * 按各钢枚数加权平均、四舍五入。counts 长度 3（钢/低碳钢/高碳钢），
     * 枚数全 0 时退回 0。铁砧锤出融合钢时直接用这份算提示，
     * 不依赖物品 NBT 是否已经刷进 CustomData。
     */
    public static int weightedBonus(int[] counts, int[] table) {
        int total = 0;
        int sum = 0;
        int n = Math.min(RATIO_KEYS.length, counts == null ? 0 : counts.length);
        for (int g = 0; g < n; g++) {
            int count = Math.max(0, counts[g]);
            total += count;
            sum += count * table[g];
        }
        return total > 0 ? Math.round((float) sum / total) : 0;
    }

    public static int weightedAttack(int[] counts) {
        return weightedBonus(counts, GRADE_ATTACK);
    }

    public static int weightedDurability(int[] counts) {
        return weightedBonus(counts, GRADE_DURABILITY);
    }

    /** 单枚该钢种的攻击基数（钢 2、低碳钢 1、高碳钢 3）。 */
    public static int gradeAttack(int grade) {
        return GRADE_ATTACK[Math.clamp(grade, 0, 2)];
    }

    /** 单枚该钢种的耐久基数（钢 2、低碳钢 3、高碳钢 1）。 */
    public static int gradeDurability(int grade) {
        return GRADE_DURABILITY[Math.clamp(grade, 0, 2)];
    }

    /** 攻击/耐久修正：混熔按各钢枚数加权平均、四舍五入；单钢与旧记录照查表。 */
    private static int ratioWeighted(ItemStack stack, int[] table) {
        CompoundTag data = data(stack);
        if (!data.contains(RATIO)) return 0;
        CompoundTag ratio = data.getCompound(RATIO);
        if (ratio.contains(RATIO_STEEL) || ratio.contains(RATIO_LOW_CARBON) || ratio.contains(RATIO_HIGH_CARBON)) {
            int[] counts = new int[RATIO_KEYS.length];
            for (int g = 0; g < RATIO_KEYS.length; g++) {
                counts[g] = Math.max(0, ratio.getInt(RATIO_KEYS[g]));
            }
            int mixed = weightedBonus(counts, table);
            // 旧存档只有 Grade、三种计数全 0：退回按钢种查表，避免显示 0。
            if (counts[0] + counts[1] + counts[2] > 0) return mixed;
        }
        return table[ratioGrade(stack)];
    }

    public static int ratioAttackBonus(ItemStack stack) {
        // 没有配比记录的刀条（木/竹刀条、未锻打的配方）修正记 0，不凭空加减。
        return ratioWeighted(stack, GRADE_ATTACK);
    }

    public static int ratioDurabilityBonus(ItemStack stack) {
        return ratioWeighted(stack, GRADE_DURABILITY);
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
