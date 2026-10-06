package cn.blockforge.generated.slashbladereshslashblad.block;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.blockentity.CustomBladeWorkbenchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 右键打开四部件自定义组装界面。 */
public final class CustomBladeWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<CustomBladeWorkbenchBlock> CODEC = simpleCodec(CustomBladeWorkbenchBlock::new);

    public CustomBladeWorkbenchBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomBladeWorkbenchBlockEntity(GeneratedMod.CUSTOM_BLADE_WORKBENCH_ENTITY.get(), pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CustomBladeWorkbenchBlockEntity bench) {
            serverPlayer.openMenu(bench, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CustomBladeWorkbenchBlockEntity bench) {
            bench.dropContents();
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
