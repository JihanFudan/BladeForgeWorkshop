package cn.blockforge.generated.slashbladereshslashblad.client;

import cn.blockforge.generated.slashbladereshslashblad.BladePartCatalog;
import cn.blockforge.generated.slashbladereshslashblad.SlashArtCatalog;
import cn.blockforge.generated.slashbladereshslashblad.menu.CustomBladeWorkbenchMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 四个外观来源与一个 SA 选择栏；所有选择仅需点击，不消耗额外物品。 */
public final class CustomBladeWorkbenchScreen extends AbstractContainerScreen<CustomBladeWorkbenchMenu> {
    private static final String[] PART_KEYS = {"blade", "guard", "sheath", "handle"};
    private static final int FIELD_WIDTH = 72;
    private static final int SA_FIELD_WIDTH = 109;
    private static final int LIST_WIDTH = 116;
    private static final int VISIBLE_ROWS = 7;
    private final int[] scrollOffsets = new int[5];
    private int openDropdown = -1;
    private List<BladePartCatalog.Entry> bladeEntries = List.of();
    private List<SlashArtCatalog.Entry> artEntries = List.of();
    private int previewPart;

    public CustomBladeWorkbenchScreen(CustomBladeWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 334;
        imageHeight = 194;
        inventoryLabelX = 28;
        inventoryLabelY = 99;
    }

    @Override protected void init() {
        super.init();
        if (minecraft != null && minecraft.player != null) {
            bladeEntries = BladePartCatalog.scan(minecraft.player.registryAccess());
            artEntries = SlashArtCatalog.scan(minecraft.player.registryAccess());
        }
        addRenderableWidget(Button.builder(Component.translatable("button.slashbladeresh_slashblad.assemble"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CustomBladeWorkbenchMenu.ASSEMBLE_BUTTON);
        }).bounds(leftPos + 168, topPos + 82, 60, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (openDropdown >= 0) {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 350.0F);
            drawOpenDropdown(graphics, openDropdown, mouseX, mouseY);
            graphics.pose().popPose();
            graphics.flush();
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF241B1B);
        graphics.fill(x + 5, y + 5, x + 241, y + 105, 0xFF4A2D2A);
        graphics.fill(x + 250, y + 5, x + imageWidth - 5, y + imageHeight - 5, 0xFF171717);
        graphics.renderOutline(x + 250, y + 5, imageWidth - 255, imageHeight - 10, 0xFF8BA9B5);
        graphics.renderOutline(x, y, imageWidth, imageHeight, 0xFFB5794E);
        for (int i = 0; i < 4; i++) {
            int sx = x + (i < 2 ? 27 : 147), sy = y + (i % 2 == 0 ? 33 : 65);
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF181818);
            graphics.renderOutline(sx, sy, 18, 18, 0xFFD0A060);
            drawClosedDropdown(graphics, i, fieldX(i), sy);
        }
        drawClosedDropdown(graphics, CustomBladeWorkbenchMenu.SA_PART,
                fieldX(CustomBladeWorkbenchMenu.SA_PART), fieldY(CustomBladeWorkbenchMenu.SA_PART));
        drawPartPreview(graphics);
    }

    private void drawPartPreview(GuiGraphics graphics) {
        int panelX = leftPos + 255, panelY = topPos + 24;
        graphics.enableScissor(panelX, panelY, panelX + 74, topPos + 184);
        if (!bladeEntries.isEmpty()) {
            int selected = Math.clamp(menu.selection(previewPart), 0, bladeEntries.size() - 1);
            CustomBladeRenderer.renderWorkbenchPreview(graphics, bladeEntries.get(selected), previewPart,
                    panelX + 37, topPos + 98, 70, 136);
        }
        graphics.disableScissor();
        graphics.renderOutline(panelX, panelY, 74, 160, 0xFF5D7782);
    }

    private void drawClosedDropdown(GuiGraphics graphics, int part, int x, int y) {
        int width = fieldWidth(part);
        graphics.fill(x, y, x + width, y + 18, 0xFF171717);
        graphics.renderOutline(x, y, width, 18, openDropdown == part ? 0xFF67D0E4 : 0xFF8BA9B5);
        String value = entryCount(part) == 0 ? Component.translatable("label.slashbladeresh_slashblad.no_model").getString()
                : entryName(part, Math.clamp(menu.selection(part), 0, entryCount(part) - 1));
        graphics.drawString(font, fit(value, width - 18), x + 3, y + 5, 0xFFE6E6E6, false);
        graphics.drawString(font, openDropdown == part ? "▲" : "▼", x + width - 11, y + 5, 0xFF67D0E4, false);
    }

    private void drawOpenDropdown(GuiGraphics graphics, int part, int mouseX, int mouseY) {
        int x = fieldX(part), y = fieldY(part) + 18;
        int count = entryCount(part);
        int visible = Math.min(VISIBLE_ROWS, count);
        int offset = Math.clamp(scrollOffsets[part], 0, Math.max(0, count - visible));
        graphics.fill(x - 1, y - 1, x + LIST_WIDTH + 1, y + visible * 14 + 1, 0xFF8BA9B5);
        for (int row = 0; row < visible; row++) {
            int index = offset + row;
            int ry = y + row * 14;
            boolean hover = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= ry && mouseY < ry + 14;
            boolean selected = index == Math.clamp(menu.selection(part), 0, Math.max(0, count - 1));
            graphics.fill(x, ry, x + LIST_WIDTH, ry + 14, hover ? 0xFF426371 : selected ? 0xFF304C59 : 0xFF202020);
            graphics.drawString(font, fit(entryName(part, index), LIST_WIDTH - 6), x + 3, ry + 3,
                    selected ? 0xFFFFD18A : 0xFFFFFFFF, false);
        }
        if (count > visible) {
            String amount = (offset + 1) + "-" + (offset + visible) + "/" + count;
            graphics.drawString(font, amount, x + LIST_WIDTH - font.width(amount) - 3, y + 3, 0xFF9FE8F5, false);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title.getString(), imageWidth - 16), 8, 7, 0xFFFFFFFF, false);
        for (int i = 0; i < 4; i++) {
            int x = i < 2 ? 8 : 128, y = i % 2 == 0 ? 23 : 55;
            graphics.drawString(font, Component.translatable("part.slashbladeresh_slashblad." + PART_KEYS[i]), x, y, 0xFFFFD18A, false);
        }
        graphics.drawString(font, Component.translatable("part.slashbladeresh_slashblad.sa"), 8, 87, 0xFFFFD18A, false);
        graphics.drawCenteredString(font, Component.translatable("label.slashbladeresh_slashblad.part_preview"), 292, 11, 0xFFFFD18A);
        graphics.drawCenteredString(font, Component.translatable("part.slashbladeresh_slashblad." + PART_KEYS[previewPart]),
                292, 174, 0xFF9FE8F5);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE8DCC8, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openDropdown >= 0) {
            int part = openDropdown;
            int x = fieldX(part), y = fieldY(part) + 18;
            int visible = Math.min(VISIBLE_ROWS, entryCount(part));
            for (int row = 0; row < visible; row++) {
                int ry = y + row * 14;
                if (mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= ry && mouseY < ry + 14) {
                    choose(part, scrollOffsets[part] + row);
                    openDropdown = -1;
                    return true;
                }
            }
        }
        for (int i = 0; i <= CustomBladeWorkbenchMenu.SA_PART; i++) {
            int x = fieldX(i), y = fieldY(i);
            if (mouseX >= x && mouseX < x + fieldWidth(i) && mouseY >= y && mouseY < y + 18) {
                openDropdown = openDropdown == i ? -1 : i;
                if (i < CustomBladeWorkbenchMenu.SA_PART) previewPart = i;
                if (openDropdown >= 0) revealSelection(i);
                return true;
            }
        }
        openDropdown = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (openDropdown >= 0 && entryCount(openDropdown) > 0) {
            int count = entryCount(openDropdown);
            int max = Math.max(0, count - Math.min(VISIBLE_ROWS, count));
            scrollOffsets[openDropdown] = Math.clamp(scrollOffsets[openDropdown] + (scrollY < 0 ? 1 : -1), 0, max);
            return true;
        }
        for (int i = 0; i <= CustomBladeWorkbenchMenu.SA_PART; i++) {
            int x = fieldX(i), y = fieldY(i), count = entryCount(i);
            if (mouseX >= x && mouseX < x + fieldWidth(i) && mouseY >= y && mouseY < y + 18 && count > 0) {
                choose(i, Math.floorMod(menu.selection(i) + (scrollY < 0 ? 1 : -1), count));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void revealSelection(int part) {
        int selected = Math.clamp(menu.selection(part), 0, Math.max(0, entryCount(part) - 1));
        if (selected < scrollOffsets[part]) scrollOffsets[part] = selected;
        if (selected >= scrollOffsets[part] + VISIBLE_ROWS) scrollOffsets[part] = selected - VISIBLE_ROWS + 1;
    }

    private void choose(int part, int value) {
        if (minecraft != null && minecraft.gameMode != null && value >= 0 && value < entryCount(part))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    CustomBladeWorkbenchMenu.SELECT_BUTTON_BASE + part * 1000 + value);
    }

    private int entryCount(int part) { return part == CustomBladeWorkbenchMenu.SA_PART ? artEntries.size() : bladeEntries.size(); }
    private int fieldWidth(int part) { return part == CustomBladeWorkbenchMenu.SA_PART ? SA_FIELD_WIDTH : FIELD_WIDTH; }
    private int fieldX(int part) {
        if (part == CustomBladeWorkbenchMenu.SA_PART) return leftPos + 49;
        return leftPos + (part < 2 ? 49 : 168);
    }
    private int fieldY(int part) {
        if (part == CustomBladeWorkbenchMenu.SA_PART) return topPos + 83;
        return topPos + (part % 2 == 0 ? 33 : 65);
    }

    private String entryName(int part, int index) {
        if (part == CustomBladeWorkbenchMenu.SA_PART) {
            SlashArtCatalog.Entry entry = artEntries.get(index);
            String translated = Component.translatable(entry.translationKey()).getString();
            return translated.equals(entry.translationKey()) ? entry.id().getPath() : translated;
        }
        BladePartCatalog.Entry entry = bladeEntries.get(index);
        String translated = Component.translatable(entry.translationKey()).getString();
        return translated.equals(entry.translationKey()) ? entry.shortName() : translated;
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }
}
