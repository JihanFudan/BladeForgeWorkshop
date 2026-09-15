package cn.blockforge.generated.slashbladereshslashblad.block;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.BladeWorkbenchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 刀剑制作台：刀条 + 刀镡 + 刀柄 + 刀鞘 四件在此组装成刀。 */
public class BladeWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<BladeWorkbenchBlock> CODEC = simpleCodec(BladeWorkbenchBlock::new);

    public BladeWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** BaseEntityBlock 默认渲染为 INVISIBLE（模型不可见），这里改回普通方块模型渲染。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 方块被移除时把台上暂存的部件掉出来，避免吞物品。 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof BladeWorkbenchBlockEntity bench) {
                bench.dropStoredItems(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BladeWorkbenchBlockEntity(GeneratedMod.BLADE_WORKBENCH_ENTITY.get(), pos, state);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                     Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BladeWorkbenchBlockEntity bench) {
            return bench.interactBlade(level, pos, player, ItemStack.EMPTY, hit).result();
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BladeWorkbenchBlockEntity bench) {
            return bench.interactBlade(level, pos, player, stack, hit);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
