package cn.blockforge.generated.slashbladereshslashblad.menu;

import java.util.List;
import cn.blockforge.generated.slashbladereshslashblad.BladeData;
import cn.blockforge.generated.slashbladereshslashblad.BladePartCatalog;
import cn.blockforge.generated.slashbladereshslashblad.GeneratedMod;
import cn.blockforge.generated.slashbladereshslashblad.SlashArtCatalog;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 四个材料槽与四个外观选择值都由服务端校验，组装按钮不会相信客户端数据。 */
public final class CustomBladeWorkbenchMenu extends AbstractContainerMenu {
    public static final int ASSEMBLE_BUTTON = 9000;
    public static final int SELECT_BUTTON_BASE = 10000;
    public static final int SA_PART = 4;
    private final Container input;
    private final ContainerData selections;
    private final ContainerLevelAccess access;

    public CustomBladeWorkbenchMenu(int id, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(4), new SimpleContainerData(5), ContainerLevelAccess.NULL);
    }

    public CustomBladeWorkbenchMenu(int id, Inventory playerInventory, Container input, ContainerData selections,
                                    ContainerLevelAccess access) {
        super(GeneratedMod.CUSTOM_BLADE_WORKBENCH_MENU.get(), id);
        checkContainerSize(input, 4);
        checkContainerDataCount(selections, 5);
        this.input = input;
        this.selections = selections;
        this.access = access;
        addDataSlots(selections);
        int[] xs = {28, 28, 148, 148};
        int[] ys = {34, 66, 34, 66};
        for (int i = 0; i < 4; i++) addSlot(new PartSlot(input, i, xs[i], ys[i]));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col + row * 9 + 9, 28 + col * 18, 111 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 28 + col * 18, 169));
    }

    public int selection(int part) { return selections.get(part); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id == ASSEMBLE_BUTTON) return assemble(player);
        if (id >= SELECT_BUTTON_BASE) {
            int packed = id - SELECT_BUTTON_BASE;
            int part = packed / 1000;
            int value = packed % 1000;
            if (part < 0 || part > SA_PART) return false;
            int entryCount = part == SA_PART ? SlashArtCatalog.scan(player.registryAccess()).size()
                    : BladePartCatalog.scan(player.registryAccess()).size();
            if (value < 0 || value >= entryCount) return false;
            selections.set(part, value);
            broadcastChanges();
            return true;
        }
        return false;
    }

    private boolean assemble(Player player) {
        for (int i = 0; i < 4; i++) if (!slots.get(i).hasItem()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "msg.slashbladeresh_slashblad.custom_missing_parts"), true);
            return false;
        }
        List<BladePartCatalog.Entry> entries = BladePartCatalog.scan(player.registryAccess());
        if (entries.isEmpty()) return false;
        BladePartCatalog.Entry[] chosen = new BladePartCatalog.Entry[4];
        for (int i = 0; i < 4; i++) chosen[i] = entries.get(Math.clamp(selections.get(i), 0, entries.size() - 1));
        boolean metalBlade = input.getItem(0).is(GeneratedMod.QUENCHED_BLADE.get());
        boolean metalGuard = isTsuba(input.getItem(1));
        int attack = 2 + (metalBlade ? 1 : 0);
        int durability = 35 + (metalGuard ? 1 : 0);
        List<SlashArtCatalog.Entry> arts = SlashArtCatalog.scan(player.registryAccess());
        net.minecraft.resources.ResourceLocation slashArt = arts.isEmpty() ? SlashArtCatalog.DEFAULT
                : arts.get(Math.clamp(selections.get(SA_PART), 0, arts.size() - 1)).id();
        ItemStack result = new ItemStack(GeneratedMod.CUSTOM_BLADE.get());
        BladeData.configureCustomBlade(result, attack, durability, chosen, slashArt);
        for (int i = 0; i < 4; i++) input.removeItem(i, 1);
        if (!player.getInventory().add(result)) player.drop(result, false);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F));
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "msg.slashbladeresh_slashblad.custom_assembled", attack, durability), true);
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int target = targetSlot(stack);
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).is(GeneratedMod.CUSTOM_BLADE_WORKBENCH.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64.0, true);
    }

    private static int targetSlot(ItemStack stack) {
        if (isBladeMaterial(stack)) return 0;
        if (isTsuba(stack)) return 1;
        if (stack.is(GeneratedMod.BLADE_SHEATH.get())) return 2;
        if (stack.is(GeneratedMod.BLADE_HANDLE.get())) return 3;
        return -1;
    }
    private static boolean isBladeMaterial(ItemStack stack) {
        return stack.is(GeneratedMod.QUENCHED_BLADE.get()) || stack.is(GeneratedMod.WOODEN_BLADE_BLANK.get())
                || stack.is(GeneratedMod.BAMBOO_BLADE_BLANK.get());
    }
    private static boolean isTsuba(ItemStack stack) {
        return stack.is(GeneratedMod.TSUBA_WOOD_IRON.get()) || stack.is(GeneratedMod.TSUBA_WOOD_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_WOOD_COPPER.get()) || stack.is(GeneratedMod.TSUBA_BAMBOO_IRON.get())
                || stack.is(GeneratedMod.TSUBA_BAMBOO_GOLD.get()) || stack.is(GeneratedMod.TSUBA_BAMBOO_COPPER.get())
                || stack.is(GeneratedMod.TSUBA_PURE_IRON.get()) || stack.is(GeneratedMod.TSUBA_PURE_GOLD.get())
                || stack.is(GeneratedMod.TSUBA_PURE_COPPER.get());
    }
    private static final class PartSlot extends Slot {
        private final int part;
        private PartSlot(Container container, int part, int x, int y) { super(container, part, x, y); this.part = part; }
        @Override public boolean mayPlace(ItemStack stack) { return targetSlot(stack) == part; }
        @Override public int getMaxStackSize() { return 1; }
    }
}
