package cn.blockforge.generated.slashbladereshslashblad.blockentity;

import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.menu.CustomBladeWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class CustomBladeWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 4;
    public static final int SELECTION_COUNT = 5;
    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
        @Override public void setChanged() { super.setChanged(); CustomBladeWorkbenchBlockEntity.this.setChanged(); }
    };
    private final int[] selections = new int[SELECTION_COUNT];
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index >= 0 && index < SELECTION_COUNT ? selections[index] : 0; }
        @Override public void set(int index, int value) { if (index >= 0 && index < SELECTION_COUNT) { selections[index] = Math.max(0, value); setChanged(); } }
        @Override public int getCount() { return SELECTION_COUNT; }
    };

    public CustomBladeWorkbenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public static CustomBladeWorkbenchBlockEntity create(BlockPos pos, BlockState state) {
        return new CustomBladeWorkbenchBlockEntity(GeneratedMod.CUSTOM_BLADE_WORKBENCH_ENTITY.get(), pos, state);
    }
    public SimpleContainer inventory() { return inventory; }
    public ContainerData selections() { return data; }

    @Override public Component getDisplayName() { return Component.translatable("container.slashbladeresh_slashblad.custom_blade_workbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CustomBladeWorkbenchMenu(id, playerInventory, inventory, data,
                net.minecraft.world.inventory.ContainerLevelAccess.create(player.level(), worldPosition));
    }

    public void dropContents() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) net.minecraft.world.Containers.dropContents(server, worldPosition, inventory);
    }

    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        for (int i = 0; i < SELECTION_COUNT; i++) selections[i] = Math.max(0, tag.getInt("Selection" + i));
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.createTag(registries));
        for (int i = 0; i < SELECTION_COUNT; i++) tag.putInt("Selection" + i, selections[i]);
    }
}
