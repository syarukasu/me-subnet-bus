package com.syaru.mesubnetbus.client;

import appeng.core.definitions.AEParts;
import com.syaru.mesubnetbus.SubnetBusMod;
import com.syaru.mesubnetbus.SubnetMenu;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SubnetScreen extends AbstractContainerScreen<SubnetMenu> {
    private static final int INK = 0x343b40;
    private final SettingButton[] modes = new SettingButton[Mode.values().length];
    private final ItemStack storageIcon = new ItemStack(AEParts.STORAGE_BUS.asItem());
    private final ItemStack interfaceIcon = new ItemStack(AEParts.INTERFACE.asItem());
    private final ItemStack busIcon = new ItemStack(SubnetBusMod.ITEM.get());
    private SettingButton include;
    private SettingButton exclude;
    private SettingButton flip;
    private Boolean previousReversed;

    public SubnetScreen(SubnetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 238;
    }

    @Override protected void init() {
        super.init();
        String[] labels = {"power", "shared", "extract", "insert"};
        ItemStack[] icons = {new ItemStack(AEParts.QUARTZ_FIBER.asItem()), storageIcon,
                new ItemStack(AEParts.IMPORT_BUS.asItem()), new ItemStack(AEParts.EXPORT_BUS.asItem())};
        for (int i = 0; i < modes.length; i++) {
            int mode = i;
            modes[i] = button(8 + i * 59, 27, 56, 21,
                    Component.translatable("gui.me_subnet_bus.mode_" + labels[i]), icons[i],
                    b -> send(SubnetMenu.SELECT_MODE + mode));
            modes[i].setTooltip(Tooltip.create(Component.translatable(Mode.values()[i].translation())));
        }
        flip = button(114, 54, 20, 21, Component.literal("\u21c4"), ItemStack.EMPTY, b -> send(SubnetMenu.FLIP));
        button(72, 115, 32, 18, Component.literal("-100"), ItemStack.EMPTY, b -> send(3));
        button(106, 115, 20, 18, Component.literal("-"), ItemStack.EMPTY, b -> send(0));
        button(182, 115, 20, 18, Component.literal("+"), ItemStack.EMPTY, b -> send(1));
        button(204, 115, 36, 18, Component.literal("+100"), ItemStack.EMPTY, b -> send(4));
        include = button(154, 78, 42, 13, Component.translatable("gui.me_subnet_bus.allow"), ItemStack.EMPTY,
                b -> send(SubnetMenu.INCLUDE));
        exclude = button(198, 78, 42, 13, Component.translatable("gui.me_subnet_bus.deny"), ItemStack.EMPTY,
                b -> send(SubnetMenu.EXCLUDE));
        include.setTooltip(Tooltip.create(Component.translatable("gui.me_subnet_bus.include")));
        exclude.setTooltip(Tooltip.create(Component.translatable("gui.me_subnet_bus.exclude")));
        previousReversed = null;
        updateControls();
    }

    private SettingButton button(int x, int y, int width, int height, Component label, ItemStack icon, Button.OnPress action) {
        return addRenderableWidget(new SettingButton(leftPos + x, topPos + y, width, height, label, icon, action));
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component cable(boolean source) {
        return Component.translatable("side.me_subnet_bus." + (source != menu.reversed() ? "inner" : "outer"));
    }

    private void updateControls() {
        for (int i = 0; i < modes.length; i++) modes[i].selected = i == menu.mode();
        include.selected = !menu.exclude();
        exclude.selected = menu.exclude();
        if (previousReversed == null || previousReversed != menu.reversed()) {
            flip.setTooltip(Tooltip.create(Component.translatable("gui.me_subnet_bus.flip")
                    .append("\n").append(Component.translatable("tooltip.me_subnet_bus.source", cable(true)))
                    .append("\n").append(Component.translatable("tooltip.me_subnet_bus.target", cable(false)))));
            previousReversed = menu.reversed();
        }
    }

    private static void recess(GuiGraphics g, int x, int y, int width, int height, int fill) {
        g.fill(x, y, x + width, y + height, 0xfff0f1f2);
        g.fill(x, y, x + width - 1, y + height - 1, 0xff73797e);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff4e5459);
        g.fill(leftPos + 1, topPos + 1, leftPos + 247, topPos + 237, 0xfff5f6f6);
        g.fill(leftPos + 3, topPos + 3, leftPos + 245, topPos + 235, 0xffd4d7d9);
        g.fill(leftPos + 3, topPos + 3, leftPos + 245, topPos + 23, 0xffe9ebec);
        g.fill(leftPos + 3, topPos + 154, leftPos + 245, topPos + 235, 0xffc3c7ca);
        g.fill(leftPos + 8, topPos + 152, leftPos + 240, topPos + 153, 0xffa3aaae);
        g.renderItem(busIcon, leftPos + 8, topPos + 5);
        g.renderItem(storageIcon, leftPos + 10, topPos + 55);
        g.renderItem(interfaceIcon, leftPos + 140, topPos + 55);
        g.fill(leftPos + 10, topPos + 72, leftPos + 26, topPos + 74, 0xff60983d);
        g.fill(leftPos + 140, topPos + 72, leftPos + 156, topPos + 74, 0xff328fa8);
        recess(g, leftPos + 128, topPos + 115, 52, 18, 0xffeef0f0);
        for (var slot : menu.slots) {
            recess(g, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18,
                    slot.index < 9 ? 0xffa8b6b7 : 0xff939a9e);
        }
        int color = switch (menu.status()) {
            case "ready" -> 0xff4d8a39;
            case "power" -> 0xff5a8191;
            case "disconnected", "no_power", "no_channel" -> 0xffaa752a;
            default -> 0xffb34e42;
        };
        g.fill(leftPos + 10, topPos + 141, leftPos + 15, topPos + 146, color);
    }

    private void label(GuiGraphics g, Component text, int x, int y, int width, int color) {
        float scale = Math.min(1f, (float) width / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        label(g, title, 29, 9, 211, INK);
        label(g, Component.translatable("gui.me_subnet_bus.storage_bus"), 30, 54, 81, 0x42672c);
        label(g, Component.translatable("gui.me_subnet_bus.interface"), 160, 54, 80, 0x24677b);
        label(g, cable(true), 30, 66, 81, INK);
        label(g, cable(false), 160, 66, 80, INK);
        label(g, Component.translatable("gui.me_subnet_bus.filters"), 8, 81, 136, INK);
        label(g, Component.translatable("gui.me_subnet_bus.priority"), 8, 120, 60, INK);
        String value = Integer.toString(menu.priority());
        float scale = Math.min(1f, 46f / Math.max(1, font.width(value)));
        g.pose().pushPose();
        g.pose().translate(154, 120, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, value, -font.width(value) / 2, 0, INK, false);
        g.pose().popPose();
        label(g, Component.translatable("status.me_subnet_bus." + menu.status()), 22, 139, 218, INK);
    }

    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        updateControls();
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
    }

    private final class SettingButton extends Button {
        private final ItemStack icon;
        private boolean selected;

        private SettingButton(int x, int y, int width, int height, Component label, ItemStack icon, OnPress action) {
            super(x, y, width, height, label, action, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hover = isHoveredOrFocused();
            g.fill(x, y, x + w, y + h, hover ? 0xff547b89 : 0xff929da2);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1,
                    selected ? 0xffccdfdc : hover ? 0xfff1f6f7 : 0xffe1e6e8);
            if (selected) g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xff397569);
            int textX = x + 4, textWidth = w - 8;
            if (!icon.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + 3, y + (h - 12) / 2f, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
                textX = x + 18;
                textWidth = w - 21;
            }
            int actual = font.width(getMessage());
            int offset = Math.max(0, (textWidth - actual) / 2);
            label(g, getMessage(), textX + offset, y + (h - 8) / 2, textWidth,
                    selected ? 0x23564d : INK);
        }

        @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
            var message = super.createNarrationMessage();
            if (selected) message.append(". ").append(Component.translatable("gui.me_subnet_bus.selected"));
            return message;
        }
    }
}
