package cn.blockforge.generated.slashbladereshslashblad;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** 原配方仍保存在配方管理器中；工作台查询被拦截，制作台使用原配方验证和生成成品。 */
public final class BladeWorkbenchRecipes {
    private BladeWorkbenchRecipes() {}

    public static final class Requirement {
        private final Ingredient ingredient;
        private final int count;
        private java.util.function.Predicate<ItemStack> predicate;
        private Requirement(java.util.function.Predicate<ItemStack> predicate) {
            this.ingredient = Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                    .map(Item::getDefaultInstance).filter(predicate));
            this.predicate = predicate;
            this.count = predicate.test(ItemStack.EMPTY) ? 0 : 1;
        }
        public Requirement(int count, Item... items) { this(count, Ingredient.of(items)); }
        public Requirement(int count, Ingredient ingredient) { this.count = count; this.ingredient = ingredient; }
        public List<Item> options() { return Arrays.stream(ingredient.getItems()).map(ItemStack::getItem).toList(); }
        public List<ItemStack> displayStacks() { return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toList(); }
        public int count() { return count; }
        public boolean matches(ItemStack stack) { return !stack.isEmpty() && (predicate == null ? ingredient.test(stack) : predicate.test(stack)); }
        public ItemStack displayStack() { return displayStacks().stream().findFirst().orElse(ItemStack.EMPTY); }
    }

    public static final class NamedRecipe {
        private final ResourceLocation id;
        private final List<Requirement> requirements;
        private final ItemStack result;
        private final Recipe original;
        private final boolean basic;
        private final int originalSlots;
        public NamedRecipe(ResourceLocation id, ItemStack result, boolean basic, Requirement... requirements) {
            this.id = id; this.result = result; this.basic = basic; this.requirements = List.of(requirements);
            this.original = null; this.originalSlots = 0;
        }
        private NamedRecipe(RecipeHolder<?> holder, Level level) {
            id = holder.id(); original = holder.value(); basic = false;
            result = original.getResultItem(level.registryAccess()).copy();
            List<Requirement> list = new ArrayList<>();
            // 保留有序的空格，以便重新组出原始有序配方并执行附属的状态检查。
            if (original instanceof SmithingRecipe smithing) {
                list.add(smithingRequirement(smithing, "getTemplate", smithing::isTemplateIngredient));
                list.add(smithingRequirement(smithing, "getBase", smithing::isBaseIngredient));
                list.add(smithingRequirement(smithing, "getAddition", smithing::isAdditionIngredient));
            } else {
                for (Object value : original.getIngredients()) {
                    Ingredient ingredient = (Ingredient) value;
                    list.add(new Requirement(ingredient.isEmpty() ? 0 : 1, ingredient));
                }
            }
            originalSlots = list.size();
            if (list.stream().noneMatch(r -> exclusively(r, GeneratedMod.QUENCHED_BLADE.get())))
                list.add(new Requirement(1, GeneratedMod.QUENCHED_BLADE.get()));
            // 名刀（包括附属模组的配方）统一使用纯金属刀镡（纯铁/纯金/纯铜），禁止木质和竹质刀镡混入。
            for (int i = 0; i < list.size(); i++) {
                if (isAnyTsuba(list.get(i))) list.set(i, guard());
            }
            if (list.stream().noneMatch(r -> exclusively(r, GeneratedMod.TSUBA_PURE_IRON.get(),
                    GeneratedMod.TSUBA_PURE_GOLD.get(), GeneratedMod.TSUBA_PURE_COPPER.get())))
                list.add(guard());
            requirements = List.copyOf(list);
        }
        private static boolean exclusively(Requirement requirement, Item... allowed) {
            List<Item> options = requirement.options();
            return requirement.count > 0 && !options.isEmpty() && options.stream().allMatch(List.of(allowed)::contains);
        }
        private static boolean isAnyTsuba(Requirement requirement) {
            Set<Item> tsuba = Set.of(GeneratedMod.TSUBA_WOOD_IRON.get(), GeneratedMod.TSUBA_WOOD_GOLD.get(),
                    GeneratedMod.TSUBA_WOOD_COPPER.get(),
                    GeneratedMod.TSUBA_BAMBOO_IRON.get(), GeneratedMod.TSUBA_BAMBOO_GOLD.get(),
                    GeneratedMod.TSUBA_BAMBOO_COPPER.get(),
                    GeneratedMod.TSUBA_PURE_IRON.get(), GeneratedMod.TSUBA_PURE_GOLD.get(),
                    GeneratedMod.TSUBA_PURE_COPPER.get());
            List<Item> options = requirement.options();
            return requirement.count > 0 && !options.isEmpty() && options.stream().anyMatch(tsuba::contains);
        }
        private static Requirement smithingRequirement(SmithingRecipe recipe, String getter,
                java.util.function.Predicate<ItemStack> fallback) {
            // 本体公开的 Ingredient 保留组件条件与 JEI 样本，其他实现用标准接口兜底。
            try {
                Object value = recipe.getClass().getMethod(getter).invoke(recipe);
                if (value instanceof Ingredient ingredient) return new Requirement(ingredient.isEmpty() ? 0 : 1, ingredient);
            } catch (NoSuchMethodException ignored) {
                // 标准 SmithingRecipe 只保证三个测试方法。
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("无法读取锻造配方材料", e);
            }
            return new Requirement(fallback);
        }
        public ResourceLocation id() { return id; }
        public String displayName() { return result.getHoverName().getString(); }
        public boolean basic() { return basic; }
        public List<Requirement> requirements() { return requirements.stream().filter(r -> r.count > 0).toList(); }
        public ItemStack output(Level level) { return result.copy(); }

        private List<ItemStack> units(List<ItemStack> stacks) {
            List<ItemStack> units = new ArrayList<>();
            for (ItemStack stack : stacks) for (int i = 0; i < stack.getCount(); i++) {
                if (units.size() >= 32) return null;
                units.add(stack.copyWithCount(1));
            }
            return units;
        }
        private List<Integer> positions() {
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < requirements.size(); i++) for (int j = 0; j < requirements.get(i).count; j++) positions.add(i);
            return positions;
        }
        private boolean assign(List<ItemStack> units, int next, List<Integer> positions, boolean[] used,
                               ItemStack[] assigned, Level level, boolean full) {
            if (next == units.size()) return !full || original == null || original.matches(input(assigned), level);
            ItemStack stack = units.get(next);
            Set<Integer> tried = new HashSet<>();
            int identicalAfter = -1;
            for (int j = 0; j < assigned.length; j++)
                if (used[j] && ItemStack.isSameItemSameComponents(assigned[j], stack)) identicalAfter = j;
            // 完全相同的物品不重复枚举交换顺序，避免条件不满足时出现阶乘级回溯。
            for (int j = identicalAfter + 1; j < positions.size(); j++) {
                int p = positions.get(j);
                if (used[j] || !tried.add(p) || !requirements.get(p).matches(stack)) continue;
                used[j] = true; assigned[j] = stack;
                if (assign(units, next + 1, positions, used, assigned, level, full)) return true;
                used[j] = false; assigned[j] = ItemStack.EMPTY;
            }
            return false;
        }
        private ItemStack[] assignment(List<ItemStack> stacks, Level level, boolean full) {
            List<ItemStack> units = units(stacks);
            List<Integer> positions = positions();
            if (units == null || units.size() > positions.size() || (full && units.size() != positions.size())) return null;
            // 优先匹配选择少的材料，避免物品标签与精确材料交叉时误判。
            units.sort(Comparator.comparingLong(s -> requirements.stream().filter(r -> r.matches(s)).count()));
            ItemStack[] assigned = new ItemStack[positions.size()]; Arrays.fill(assigned, ItemStack.EMPTY);
            return assign(units, 0, positions, new boolean[positions.size()], assigned, level, full) ? assigned : null;
        }
        private RecipeInput input(ItemStack[] assigned) {
            if (original instanceof SmithingRecipe) {
                ItemStack[] grid = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
                List<Integer> positions = positions();
                for (int i = 0; i < positions.size(); i++) if (positions.get(i) < 3) grid[positions.get(i)] = assigned[i];
                return new SmithingRecipeInput(grid[0], grid[1], grid[2]);
            }
            int width = original instanceof ShapedRecipe shaped ? shaped.getWidth() : Math.max(1, Math.min(3, originalSlots));
            int height = original instanceof ShapedRecipe shaped ? shaped.getHeight() : Math.max(1, (originalSlots + width - 1) / width);
            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(width * height, ItemStack.EMPTY));
            List<Integer> positions = positions();
            for (int i = 0; i < positions.size(); i++) if (positions.get(i) < originalSlots) grid.set(positions.get(i), assigned[i]);
            return CraftingInput.of(width, height, grid);
        }
        public boolean acceptsPartial(List<ItemStack> stacks) { return assignment(stacks, null, false) != null; }
        public boolean matches(List<ItemStack> stacks, Level level) { return assignment(stacks, level, true) != null; }
        public ItemStack assemble(List<ItemStack> stacks, Level level) {
            ItemStack[] assigned = assignment(stacks, level, true);
            if (assigned == null) return ItemStack.EMPTY;
            return original == null ? result.copy() : original.assemble(input(assigned), level.registryAccess());
        }
        public List<ItemStack> remainders(List<ItemStack> stacks, Level level) {
            ItemStack[] assigned = assignment(stacks, level, true);
            if (original == null || assigned == null) return List.of();
            return original.getRemainingItems(input(assigned));
        }
        public String missing(List<ItemStack> stacks) {
            return displayName() + "：材料或原刀条件尚未齐全，请按 JEI 配方继续放入；升级条件沿用原配方。";
        }
    }

    private static Requirement guard() {
        return new Requirement(1, GeneratedMod.TSUBA_PURE_IRON.get(), GeneratedMod.TSUBA_PURE_GOLD.get(),
                GeneratedMod.TSUBA_PURE_COPPER.get());
    }
    private static Item item(String id) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
    private static Requirement r(int n, String id) { return new Requirement(n, item(id)); }
    private static NamedRecipe built(String id, ItemStack result, boolean basic, Requirement... r) {
        return new NamedRecipe(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, id), result, basic, r);
    }
    private record Cache(List<RecipeHolder<?>> source, List<NamedRecipe> recipes) {}
    private static final Map<RecipeManager, Cache> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    public static List<NamedRecipe> namedRecipes(Level level) {
        List<RecipeHolder<?>> source = level == null ? List.of() : List.copyOf(level.getRecipeManager().getRecipes());
        if (level != null) {
            Cache cached = CACHE.get(level.getRecipeManager());
            if (cached != null && cached.source.equals(source)) return cached.recipes;
        }
        List<NamedRecipe> list = new ArrayList<>();
        // 木刀与竹刀不再走刀剑制作台：改为手持刀条右键现场组装（见 BladeAssembly）。
        Requirement blank = new Requirement(1, GeneratedMod.QUENCHED_BLADE.get());
        list.add(built("silver_bamboo", BladeData.sword(BladeData.SILVERBAMBOO), false,
                r(1,"slashblade:slashblade_bamboo"), blank, guard(), new Requirement(1,Items.PAPER), new Requirement(1,Items.BLACK_DYE)));
        list.add(built("white_sheath", BladeData.sword(BladeData.WHITE), false,
                r(1,"slashblade:slashblade_wood"), r(2,"slashblade:proudsoul_ingot"), new Requirement(1,Items.GOLD_INGOT), blank, guard()));
        list.add(built("mumei", BladeData.sword(BladeData.MUMEI), false,
                r(1,"slashblade:slashblade_white"), new Requirement(1,Items.BLUE_DYE), new Requirement(1,Items.COAL_BLOCK),
                guard(), new Requirement(1,Items.BLAZE_ROD), new Requirement(1,Items.GOLD_INGOT), blank));
        if (level != null) {
            ItemStack ruby = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"ruby_definition"))
                    .map(h -> h.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
            if (!ruby.isEmpty()) list.add(built("ruby", ruby, false, r(1,"slashblade:slashblade_silverbamboo"),
                    r(1,"slashblade:proudsoul_ingot"), r(1,"slashblade:proudsoul"), new Requirement(1,Items.RED_DYE), guard(), blank));
            for (RecipeHolder<?> holder : source) {
                if (holder.id().equals(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"ruby_definition"))) continue;
                if (shouldMove(holder.value(), level.registryAccess())
                        && (holder.value() instanceof SmithingRecipe || !holder.value().getIngredients().isEmpty())) {
                    // 使用与内置条目相同的 ID 可完全覆盖内置材料清单。
                    list.removeIf(recipe -> recipe.id.equals(holder.id()));
                    list.add(new NamedRecipe(holder, level));
                }
            }
            list.sort(Comparator.comparing(recipe -> recipe.id.toString()));
            CACHE.put(level.getRecipeManager(), new Cache(source, List.copyOf(list)));
        }
        return list;
    }
    public static boolean shouldMove(Recipe<?> recipe, net.minecraft.core.HolderLookup.Provider registries) {
        return (recipe instanceof CraftingRecipe || recipe instanceof SmithingRecipe)
                && BladeData.isSlashBlade(recipe.getResultItem(registries));
    }
    public static NamedRecipe find(List<ItemStack> stacks, Level level) {
        for (NamedRecipe recipe : namedRecipes(level)) if (recipe.matches(stacks, level)) return recipe;
        return null;
    }
}
