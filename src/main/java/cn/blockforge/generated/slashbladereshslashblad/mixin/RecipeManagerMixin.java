package cn.blockforge.generated.slashbladereshslashblad.mixin;

import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Optional;

/** 从所有常规配方查询入口阻止拔刀剑在工作台生成，不删除原配方或破坏客户端同步。 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    /**
     * 判断是否要屏蔽。整个过程必须"永不抛出"：这里会被每次工作台/熔炉/铁砧的配方查询调用，
     * 任何一条模组配方的 assemble 或 getResultItem 抛出（例如前置的 shaped_blade
     * 在空材料格上取不到输出刀），都会顺着查询把玩家的存档操作直接炸掉。
     * 出现异常时按"不屏蔽"处理，宁可漏过滤也不崩游戏。
     */
    private static <I extends RecipeInput> boolean blocked(RecipeHolder<Recipe<I>> holder, I input, Level level) {
        try {
            if (level == null || input == null || holder == null) return false;
            if (!(holder.value() instanceof CraftingRecipe) && !(holder.value() instanceof SmithingRecipe)) return false;
            return BladeData.isSlashBlade(holder.value().getResultItem(level.registryAccess()))
                    || BladeData.isSlashBlade(holder.value().assemble(input, level.registryAccess()));
        } catch (RuntimeException e) {
            return false;
        }
    }
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput> void blockSimple(RecipeType<?> type, I input, Level level,
            CallbackInfoReturnable<Optional<RecipeHolder<Recipe<I>>>> cir) {
        if (cir.getReturnValue().filter(h -> blocked(h, input, level)).isPresent()) cir.setReturnValue(Optional.empty());
    }
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput> void blockId(RecipeType<?> type, I input, Level level, net.minecraft.resources.ResourceLocation id,
            CallbackInfoReturnable<Optional<RecipeHolder<Recipe<I>>>> cir) {
        if (cir.getReturnValue().filter(h -> blocked(h, input, level)).isPresent()) cir.setReturnValue(Optional.empty());
    }
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput> void blockCached(RecipeType<?> type, I input, Level level, RecipeHolder<?> cached,
            CallbackInfoReturnable<Optional<RecipeHolder<Recipe<I>>>> cir) {
        if (cir.getReturnValue().filter(h -> blocked(h, input, level)).isPresent()) cir.setReturnValue(Optional.empty());
    }
    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput> void blockList(RecipeType<?> type, I input, Level level,
            CallbackInfoReturnable<List<RecipeHolder<Recipe<I>>>> cir) {
        cir.setReturnValue(cir.getReturnValue().stream().filter(h -> !blocked(h, input, level)).toList());
    }
}
